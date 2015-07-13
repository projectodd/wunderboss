;; Copyright 2014-2015 Red Hat, Inc, and individual contributors.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns wunderboss.messaging-tests
  (:require [clojure.test :refer :all])
  (:import [org.projectodd.wunderboss Option WunderBoss]
           [org.projectodd.wunderboss.codecs Codecs None StringCodec]
           [org.projectodd.wunderboss.messaging Messaging
            Context Context$Mode
            ConcreteReply
            Destination Destination$ListenOption Destination$ReceiveOption
            Destination$PublishOption
            Queue
            Queue$RespondOption
            Topic$SubscribeOption Topic$UnsubscribeOption
            Messaging$CreateContextOption Messaging$CreateQueueOption
            MessageHandler]
           java.util.concurrent.TimeUnit))

(def default (doto (WunderBoss/findOrCreateComponent Messaging) (.start)))

(defn create-opts-fn [class]
  ;; clojure 1.7.0 no longer initializes classes on import, so we have
  ;; to force init here (see CLJ-1315)
  (Class/forName (.getName class))
  (let [avail-options (->> class
                        Option/optsFor
                        (map #(vector (keyword (.name %)) %))
                        (into {}))]
    (fn [opts]
      (reduce (fn [m [k v]]
                (assoc m
                  (if-let [enum (avail-options k)]
                    enum
                    (throw (IllegalArgumentException. (str k " is not a valid option."))))
                  v))
        {}
        opts))))

(def coerce-queue-options (create-opts-fn Messaging$CreateQueueOption))
(def coerce-context-options (create-opts-fn Messaging$CreateContextOption))
(def coerce-listen-options (create-opts-fn Destination$ListenOption))
(def coerce-respond-options (create-opts-fn Queue$RespondOption))
(def coerce-receive-options (create-opts-fn Destination$ReceiveOption))
(def coerce-publish-options (create-opts-fn Destination$PublishOption))
(def coerce-subscribe-options (create-opts-fn Topic$SubscribeOption))
(def coerce-unsubscribe-options (create-opts-fn Topic$UnsubscribeOption))

(def frob-codec
  (proxy [StringCodec] ["frob" "application/frob"]
    (encode [ data]
      (format "FROBBED %s" data))
    (decode [data]
      (format "DEFROBBED %s" data))))

(def codecs (-> (Codecs.) (.add None/INSTANCE) (.add frob-codec)))

(defn handler [f]
  (reify MessageHandler
    (onMessage [_ m _]
      (ConcreteReply. (f (.body m)) nil))))

(defn create-queue [& opts]
  (let [[name opts] opts
        name (or name (str (java.util.UUID/randomUUID)))]
    (.findOrCreateQueue default name
      (coerce-queue-options (if (:context opts)
                              opts
                              (merge opts {:durable false}))))))

(defn create-topic [name]
  (.findOrCreateTopic default name nil))

(defmacro throws-with-cause [clazz & body]
  `(try
     ~@body
     (catch Exception e#
       (is (instance? ~clazz (.getCause e#))))))

(deftest queue-creation-publish-receive-close
  (let [queue (create-queue "a-queue")]

    ;; findOrCreateQueue should return the same queue for the same name
    (is (= (.jmsDestination queue)
          (.jmsDestination (create-queue "a-queue"))))

    ;; we should be able to publish and rcv
    (.publish queue "hi" None/INSTANCE nil)
    (let [msg (.receive queue codecs (coerce-receive-options {:timeout 1000}))]
      (is msg)
      (is (= "hi" (.body msg))))

    ;; a stopped queue should no longer be avaiable
    (.stop queue)
    (is (thrown? javax.jms.InvalidDestinationException
          (.receive queue codecs (coerce-receive-options {:timeout 1}))))))

(deftest publish-should-use-the-passed-context
  (let [c (.createContext default nil)
        q (create-queue "publish-c")]
    (.close c)
    (throws-with-cause javax.jms.IllegalStateException
      (.publish q "boom" None/INSTANCE
        (coerce-publish-options {:context c})))))

(deftest publish-should-encode-with-the-given-codec-and-receive-should-find-the-right-one
  (let [q (create-queue)]
    (.publish q "hi" frob-codec nil)
    (is (= "DEFROBBED FROBBED hi" (.body (.receive q codecs nil))))))

(deftest request-should-use-the-passed-context
  (let [c (.createContext default (coerce-context-options {:host "localhost"}))
        q (create-queue "publish-c")]
    (.close c)
    (try
      (.request q "boom" None/INSTANCE codecs
        (coerce-publish-options {:context c}))
      (catch Exception e
        (is (instance? javax.jms.IllegalStateException (-> e .getCause .getCause)))))))

(deftest receive-should-use-the-passed-context
  (let [c (.createContext default nil)
        q (create-queue "receive-context")]
    (.close c)
    (throws-with-cause javax.jms.IllegalStateException
      (.receive q codecs (coerce-receive-options {:context c})))))

(deftest listen-should-use-the-passed-context
  (let [c (.createContext default (coerce-context-options {:host "localhost"}))
        q (create-queue "listen-context")]
    (.close c)
    (throws-with-cause javax.jms.IllegalStateException
      (.listen q (handler identity) codecs (coerce-listen-options {:context c})))))

(deftest respond-should-use-the-passed-context
  (let [c (.createContext default (coerce-context-options {:host "localhost"}))
        q (create-queue "listen-context")]
    (.close c)
    (throws-with-cause javax.jms.IllegalStateException
      (.respond q (handler identity) codecs (coerce-listen-options {:context c})))))

(deftest subscribe-should-use-the-passed-context
  (let [c (.createContext default (coerce-context-options {:client_id "ham"}))
        t (create-topic "subscribe-context")]
    (.close c)
    (throws-with-cause javax.jms.IllegalStateException
      (.subscribe t "ham" (handler identity) codecs (coerce-subscribe-options {:context c})))))

(deftest unsubscribe-should-use-the-passed-context
  (let [c (.createContext default (coerce-context-options {:client_id "ham"}))
        t (create-topic "subscribe-context")]
    (.close c)
    (throws-with-cause javax.jms.IllegalStateException
      (.unsubscribe t "ham" (coerce-unsubscribe-options {:context c})))))

(deftest closing-a-listener-should-work
  (let [queue (create-queue "listen-queue")]
    (let [called (atom (promise))
          listener (.listen queue
                     (handler
                       (fn [msg]
                         (deliver @called msg)))
                     codecs
                     nil)]
      (.publish queue "hi" None/INSTANCE nil)
      (is (= "hi" (deref @called 1000 :failure)))
      (reset! called (promise))
      (.close listener)
      (.publish queue "hi" None/INSTANCE nil)
      (is (= :success (deref @called 1000 :success))))))

(deftest subscribe-to-topic
  (let [topic (create-topic "subscribe-topic")
        called (atom (promise))
        listener (.subscribe topic
                   "my-sub"
                   (handler
                     (fn [msg]
                       (deliver @called msg)))
                   codecs
                   nil)]
    (.publish topic "hi" None/INSTANCE nil)
    (is (= "hi" (deref @called 1000 :failure)))
    (reset! called (promise))
    (.close listener)
    (.publish topic "hi-again" None/INSTANCE nil)
    (is (= :failure (deref @called 100 :failure)))
    (with-open [listener (.subscribe topic
                           "my-sub"
                           (handler
                             (fn [msg]
                               (deliver @called msg)))
                           codecs
                           nil)]
      (is (= "hi-again" (deref @called 1000 :failure))))
    (.unsubscribe topic "my-sub" nil)))

(deftest unsubscribe-from-topic
  (let [topic (create-topic "subscribe-topic")
        called (atom (promise))
        listener (.subscribe topic
                   "another-sub"
                   (handler
                     (fn [msg]
                       (deliver @called msg)))
                   codecs
                   nil)]
    (.publish topic "hi" None/INSTANCE nil)
    (is (= "hi" (deref @called 1000 :failure)))
    (reset! called (promise))
    (.close listener)
    (.unsubscribe topic "another-sub" nil)
    (.publish topic "failure" None/INSTANCE nil)
    (is (= :success (deref @called 100 :success)))
    (with-open [listener (.subscribe topic
                           "another-sub"
                           (handler
                             (fn [msg]
                               (deliver @called msg)))
                           codecs
                           nil)]
      (is (= :success (deref @called 100 :success))))))

(deftest listen-with-concurrency
  (let [queue (create-queue "listen-queue")
        latch (java.util.concurrent.CountDownLatch. 5)
        listener (.listen queue
                   (handler
                     (fn [msg]
                       (let [msg (read-string msg)]
                         (.countDown latch)
                         (.await latch))))
                   codecs
                   (coerce-listen-options {:concurrency 5}))]
    (dotimes [n 5]
      (.publish queue (str n) None/INSTANCE nil))
    (is (.await latch 10 TimeUnit/SECONDS))
    (.close listener)))

(deftest request-response
  (let [queue (create-queue "rr-queue")]
    (with-open [listener (.respond queue
                           (handler identity)
                           codecs
                           nil)]
      (let [response (.request queue "hi" None/INSTANCE codecs nil)]
        (is (= "hi" (.body (.get response))))
        ;; result should be cached
        (is (= "hi" (.body (.get response))))))))

(deftest request-response-should-coordinate-requests-with-responses
  (let [queue (create-queue "rr-coord-queue")]
    (with-open [listener (.respond queue
                           (handler
                             (fn [msg]
                               (let [time (read-string msg)]
                                 (Thread/sleep time)
                                 (str "response-" time))))
                           codecs
                           (coerce-respond-options {:concurrency 5}))]
      (let [response1 (.request queue "50" None/INSTANCE codecs nil)
            response2 (.request queue "100" None/INSTANCE codecs nil)
            response3 (.request queue "25" None/INSTANCE codecs nil)]
        (is (= "response-50" (.body (.get response1))))
        (is (= "response-100" (.body (.get response2))))
        (is (= "response-25" (.body (.get response3))))))))

(deftest request-response-with-ttl
  (let [queue (create-queue "rr-queue")
        latch (java.util.concurrent.CountDownLatch. 1)]
    (with-open [listener (.respond queue
                           (handler (fn [m]
                                      (.countDown latch)
                                      m))
                           codecs
                           (coerce-respond-options {:ttl 1}))]
      (let [response (.request queue "nope" None/INSTANCE codecs nil)]
        (.await latch 10 TimeUnit/SECONDS)
        (Thread/sleep 100)
        (is (thrown? java.util.concurrent.TimeoutException
              (.get response 1 TimeUnit/MILLISECONDS)))))))

(deftest context-rollback
  (let [c (.createContext default
            (coerce-context-options {:mode Context$Mode/TRANSACTED}))
        q (create-queue "rollback")]
    (.publish q "failure" None/INSTANCE
      (coerce-publish-options {:context c}))
    (.rollback c)
    (is (not (.receive q codecs (coerce-receive-options {:timeout 1000}))))
    (.close c)))

(deftest remote-contexts
  ;; this creates the queue in the 'remote' broker
  (create-queue "remote-queue")
  (with-open [c (.createContext default
                  (coerce-context-options {:host "localhost"}))]
    (let [q (create-queue "remote-queue" {:context c})]
      (.publish q "success" None/INSTANCE
        (coerce-publish-options {:context c}))
      (let [msg (.receive q codecs (coerce-receive-options {:context c
                                                            :timeout 1000}))]
        (is msg)
        (is (= "success" (.body msg)))))))

(deftest publish-receive-with-the-same-context-should-work
  (with-open [ctx (.createContext default nil)]
    (let [q (create-queue)]
      (.publish q "success" None/INSTANCE (coerce-publish-options {:context ctx}))
      (is (= "success"
            (.body (.receive q codecs (coerce-receive-options {:context ctx}))))))))

(deftest publish-and-receive-inside-a-non-transactional-listener-should-work
  (let [q1 (create-queue)
        q2 (create-queue)
        p  (promise)
        l  (.listen q1
             (handler
               (fn [msg]
                 (.publish q2 msg None/INSTANCE nil)
                 (when-let [result (.receive q2 codecs (coerce-receive-options {:timeout 1000}))]
                   (is (= msg (.body result)))
                   (deliver p (.body result)))))
             codecs
             (coerce-listen-options {:mode Context$Mode/AUTO_ACK}))]
    (.publish q1 "whatevs" None/INSTANCE nil)
    (is (= "whatevs" (deref p 1000 :failure)))))

(deftest toss-when-xa-unavailable
  (is (thrown-with-msg? NullPointerException
        #"TransactionManager not found"
        (.createContext default (coerce-context-options {:xa true})))))
