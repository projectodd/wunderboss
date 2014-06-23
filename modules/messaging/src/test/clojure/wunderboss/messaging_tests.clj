;; Copyright 2014 Red Hat, Inc, and individual contributors.
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
            Connection
            Destination Destination$ListenOption Destination$ReceiveOption
            Destination$SendOption
            Queue
            Queue$RespondOption
            Topic$SubscribeOption Topic$UnsubscribeOption
            Messaging$CreateConnectionOption Messaging$CreateQueueOption
            MessageHandler
            Session$Mode
            Connection$CreateSessionOption]
           java.util.concurrent.TimeUnit))

(System/setProperty "logging.configuration" "file:///home/tcrawley/tmp/hornetq-logging.properties")
(System/setProperty "java.util.logging.manager" "org.jboss.logmanager.LogManager")

(def default (doto (WunderBoss/findOrCreateComponent Messaging) (.start)))

(defn create-opts-fn [class]
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
(def coerce-connection-options (create-opts-fn Messaging$CreateConnectionOption))
(def coerce-listen-options (create-opts-fn Destination$ListenOption))
(def coerce-respond-options (create-opts-fn Queue$RespondOption))
(def coerce-receive-options (create-opts-fn Destination$ReceiveOption))
(def coerce-send-options (create-opts-fn Destination$SendOption))
(def coerce-subscribe-options (create-opts-fn Topic$SubscribeOption))
(def coerce-unsubscribe-options (create-opts-fn Topic$UnsubscribeOption))
(def coerce-session-options (create-opts-fn Connection$CreateSessionOption))

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
      (f (.body m)))))

(defn create-queue [& opts]
  (let [[name opts] opts
        name (or name (str (java.util.UUID/randomUUID)))]
    (.findOrCreateQueue default name
      (coerce-queue-options (merge opts {:durable false})))))

(defn create-topic [name]
  (.findOrCreateTopic default name nil))

(deftest queue-creation-send-receive-close
  (let [queue (create-queue "a-queue")]

    ;; findOrCreateQueue should return the same queue for the same name
    (is (= (.destination queue)
          (.destination (create-queue "a-queue"))))

    ;; we should be able to send and rcv
    (.send queue "hi" None/INSTANCE nil)
    (let [msg (.receive queue codecs (coerce-receive-options {:timeout 1000}))]
      (is msg)
      (is (= "hi" (.body msg))))

    ;; a stopped queue should no longer be avaiable
    (.stop queue)
    (is (thrown? javax.jms.InvalidDestinationRuntimeException
          (.receive queue codecs (coerce-receive-options {:timeout 1}))))))

(deftest send-should-use-the-passed-connection
  (let [c (.createConnection default nil)
        q (create-queue "send-c")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.send q "boom" None/INSTANCE
            (coerce-send-options {:connection c}))))))

(deftest send-should-encode-with-the-given-codec-and-receive-should-find-the-right-one
  (let [q (create-queue)]
    (.send q "hi" frob-codec nil)
    (is (= "DEFROBBED FROBBED hi" (.body (.receive q codecs nil))))))


(deftest send-should-use-the-passed-session
  (let [s (.createSession (.defaultConnection default) nil)
        q (create-queue "send-c")]
    (.close s)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.send q "boom" None/INSTANCE
            (coerce-send-options {:session s}))))))

(deftest send-should-not-close-the-passed-session
  (let [s (.createSession (.defaultConnection default) nil)
        q (create-queue "send-close-session")
        check-fn (fn []
                   (.send q "success" None/INSTANCE
                     (coerce-send-options {:session s}))
                   (is (= "success" (.body (.receive q codecs nil)))))]
    (check-fn)
    (check-fn)
    (.close s)))

(deftest request-should-use-the-passed-connection
  (let [c (.createConnection default nil)
        q (create-queue "send-c")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.request q "boom" None/INSTANCE
            (coerce-send-options {:connection c}))))))

(deftest request-should-use-the-passed-session
  (let [s (.createSession (.defaultConnection default) nil)
        q (create-queue "send-c")]
    (.close s)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.request q "boom" None/INSTANCE
            (coerce-send-options {:session s}))))))

(deftest request-should-not-close-the-passed-session
  (let [s (.createSession (.defaultConnection default) nil)
        q (create-queue "send-close-session")
        r (.respond q
            (handler identity)
            codecs
            nil)
        check-fn (fn []
                   (let [response (.request q "success" None/INSTANCE
                                    (coerce-send-options {:session s}))]
                     (is (= "success" (.body (deref response 1000 :failure))))))]
    (check-fn)
    (check-fn)
    (.close r)
    (.close s)))

(deftest receive-should-use-the-passed-connection
  (let [c (.createConnection default nil)
        q (create-queue "receive-connection")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.receive q codecs (coerce-receive-options {:connection c}))))))

(deftest receive-should-use-the-passed-session
  (let [s (.createSession (.defaultConnection default) nil)
        q (create-queue "receive-session")]
    (.close s)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.receive q codecs (coerce-receive-options {:session s}))))))

(deftest receive-should-not-close-the-passed-session
  (let [s (.createSession (.defaultConnection default) nil)
        q (create-queue "receive-close-session")
        check-fn (fn []
                   (.send q "success" None/INSTANCE nil)
                   (is (= "success"
                         (.body (.receive q codecs (coerce-receive-options {:session s}))))))]
    (check-fn)
    (check-fn)
    (.close s)))

(deftest listen-should-use-the-passed-connection
  (let [c (.createConnection default nil)
        q (create-queue "listen-connection")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.listen q (handler identity) codecs (coerce-listen-options {:connection c}))))))

(deftest respond-should-use-the-passed-connection
  (let [c (.createConnection default nil)
        q (create-queue "listen-connection")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.respond q (handler identity) codecs (coerce-listen-options {:connection c}))))))

(deftest subscribe-should-use-the-passed-connection
  (let [c (.createConnection default (coerce-connection-options {:client_id "ham"}))
        t (create-topic "subscribe-connection")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.subscribe t "ham" (handler identity) codecs (coerce-subscribe-options {:connection c}))))))

(deftest unsubscribe-should-use-the-passed-connection
  (let [c (.createConnection default (coerce-connection-options {:client_id "ham"}))
        t (create-topic "subscribe-connection")]
    (.close c)
    (is (thrown? javax.jms.IllegalStateRuntimeException
          (.unsubscribe t "ham" (coerce-unsubscribe-options {:connection c}))))))

(deftest closing-a-listener-should-work
  (let [queue (create-queue "listen-queue")]
    (let [called (atom (promise))
          listener (.listen queue
                     (handler
                       (fn [msg]
                         (deliver @called msg)))
                     codecs
                     nil)]
      (.send queue "hi" None/INSTANCE nil)
      (is (= "hi" (deref @called 1000 :failure)))
      (reset! called (promise))
      (.close listener)
      (.send queue "hi" None/INSTANCE nil)
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
    (.send topic "hi" None/INSTANCE nil)
    (is (= "hi" (deref @called 1000 :failure)))
    (reset! called (promise))
    (.close listener)
    (.send topic "hi-again" None/INSTANCE nil)
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
    (.send topic "hi" None/INSTANCE nil)
    (is (= "hi" (deref @called 1000 :failure)))
    (reset! called (promise))
    (.close listener)
    (.unsubscribe topic "another-sub" nil)
    (.send topic "failure" None/INSTANCE nil)
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
      (.send queue (str n) None/INSTANCE nil))
    (is (.await latch 10 TimeUnit/SECONDS))
    (.close listener)))

(deftest request-response
  (let [queue (create-queue "rr-queue")]
    (with-open [listener (.respond queue
                           (handler identity)
                           codecs
                           nil)]
      (let [response (.request queue "hi" None/INSTANCE nil)]
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
      (let [response1 (.request queue "50" None/INSTANCE nil)
            response2 (.request queue "100" None/INSTANCE nil)
            response3 (.request queue "25" None/INSTANCE nil)]
        (is (= "response-50" (.body (.get response1))))
        (is (= "response-100" (.body (.get response2))))
        (is (= "response-25" (.body (.get response3))))))))

(deftest request-response-with-ttl
  (let [queue (create-queue "rr-queue")]
    (with-open [listener (.respond queue
                           (handler identity)
                           codecs
                           (coerce-respond-options {:ttl 1}))]
      (let [response (.request queue "nope" None/INSTANCE nil)]
        (Thread/sleep 100)
        (is (thrown? java.util.concurrent.TimeoutException
              (.get response 1 TimeUnit/MILLISECONDS)))))))

(deftest session-rollback
  (let [s (.createSession (.defaultConnection default)
            (coerce-session-options {:mode Session$Mode/TRANSACTED}))
        q (create-queue "rollback")]
    (.send q "failure" None/INSTANCE
      (coerce-send-options {:session s}))
    (.rollback s)
    (is (not (.receive q codecs (coerce-receive-options {:timeout 1000}))))
    (.close s)))

(deftest remote-connections
  ;; this creates the queue in the 'remote' broker
  (create-queue "remote-queue")
  (with-open [c (.createConnection default
                  (coerce-connection-options {:host "localhost"}))]
    (let [q (create-queue "remote-queue" {:connection c})]
      (.send q "success" None/INSTANCE
        (coerce-send-options {:connection c}))
      (let [msg (.receive q codecs (coerce-receive-options {:connection c
                                                            :timeout 1000}))]
        (is msg)
        (is (= "success" (.body msg)))))))

(deftest send-receive-with-the-same-session-should-work
  (with-open [s (.createSession (.defaultConnection default) nil)]
    (let [q (create-queue)]
      (.send q "success" None/INSTANCE (coerce-send-options {:session s}))
      (is (= "success"
            (.body (.receive q codecs (coerce-receive-options {:session s}))))))))

(deftest send-and-receive-inside-a-non-transactional-listener-should-work
  (let [q1 (create-queue)
        q2 (create-queue)
        p  (promise)
        l  (.listen q1
             (handler
               (fn [msg]
                 (.send q2 msg None/INSTANCE nil)
                 (when-let [result (.receive q2 codecs (coerce-receive-options {:timeout 1000}))]
                   (is (= msg (.body result)))
                   (deliver p (.body result)))))
             codecs
             (coerce-listen-options {:transacted false}))]
    (.send q1 "whatevs" None/INSTANCE nil)
    (is (= "whatevs" (deref p 1000 :failure)))))
