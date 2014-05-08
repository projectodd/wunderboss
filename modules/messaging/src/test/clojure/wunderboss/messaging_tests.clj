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
           [org.projectodd.wunderboss.messaging Messaging
            Connection Connection$ListenOption Connection$ReceiveOption
            Connection$RespondOption Connection$SendOption
            Messaging$CreateConnectionOption Messaging$CreateEndpointOption
            MessageHandler]
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

(def coerce-endpoint-options (create-opts-fn Messaging$CreateEndpointOption))
(def coerce-connection-options (create-opts-fn Messaging$CreateConnectionOption))
(def coerce-listen-options (create-opts-fn Connection$ListenOption))
(def coerce-respond-options (create-opts-fn Connection$RespondOption))
(def coerce-receive-options (create-opts-fn Connection$ReceiveOption))
(def coerce-send-options (create-opts-fn Connection$SendOption))

(defn create-endpoint [name & [opts]]
  (.findOrCreateEndpoint default "a-queue"
    (coerce-endpoint-options (merge opts {:durable false}))))

(deftest endpoint-creation-should-throw-if-a-durable-broadcast-is-requested
  (is (thrown? IllegalArgumentException
        (.findOrCreateEndpoint default "foo"
          (coerce-endpoint-options {:broadcast true :durable true})))))

(deftest endpoint-creation-should-throw-if-a-broadcast-is-requested-and-selector-is-passed
  (is (thrown? IllegalArgumentException
        (.findOrCreateEndpoint default "foo"
          (coerce-endpoint-options {:broadcast true :selector "foo"})))))

(deftest queue-creation-publish-receive-close
  (with-open [connection (.createConnection default nil)
              endpoint (create-endpoint "a-queue")]

    ;; findOrCreateEndpoint should return the same queue for the same name
    (is (= (.destination endpoint)
          (.destination (create-endpoint "a-queue"))))

    ;; we should be able to send and rcv
    (.send connection endpoint "hi" nil nil)
    (let [msg (.receive connection endpoint (coerce-receive-options {:timeout 1000}))]
      (is msg)
      (is (= "hi" (.body msg String))))

    ;; a closed endpoint should no longer be avaiable
    (.close endpoint)
    (is (thrown? javax.jms.InvalidDestinationException
          (.receive connection endpoint (coerce-receive-options {:timeout 1}))))))

(deftest receive-with-a-durable-subscription
  (with-open [topic (create-endpoint "subs" {:broadcast true})
              connection (.createConnection default (coerce-connection-options {:client_id "client-id"}))]
    (.send connection topic "hi" nil nil)
    (is (nil? (.receive connection topic
                (coerce-receive-options {:timeout -1 :subscriber_name "bar"}))))
    (.send connection topic "hi2" nil nil)
    (is (= "hi2" (.body
                   (.receive connection topic
                     (coerce-receive-options {:timeout 100 :subscriber_name "bar"}))
                   String)))))

(deftest listen
  (with-open [connection (.createConnection default nil)
              queue (create-endpoint "listen-queue")]
    (let [called (atom (promise))
          listener (.listen connection queue
                     (reify MessageHandler
                       (onMessage [_ msg]
                         (deliver @called (.body msg String))))
                     nil)]
      (.send connection queue "hi" nil nil)
      (is (= "hi" (deref @called 1000 :failure)))
      (reset! called (promise))
      (.close listener)
      (.send connection queue "hi" nil nil)
      (is (= :success (deref @called 1000 :success))))))

(deftest listen-with-durable-subscriber
  (with-open [connection (.createConnection default
                           (coerce-connection-options {:client_id "something"}))
              topic (create-endpoint "listen-topic" {:broadcast true})]
    (let [called (atom (promise))
          listener (.listen connection topic
                     (reify MessageHandler
                       (onMessage [_ msg]
                         (deliver @called (.body msg String))))
                     (coerce-listen-options {:subscriber_name "subs"}))]
      (.send connection topic "hi" nil nil)
      (is (= "hi" (deref @called 1000 :failure)))
      (reset! called (promise))
      (.close listener)
      (.send connection topic "hi-again" nil nil)
      (is (= :failure (deref @called 100 :failure)))
      (with-open [listener (.listen connection topic
                             (reify MessageHandler
                               (onMessage [_ msg]
                                 (deliver @called (.body msg String))))
                             (coerce-listen-options {:subscriber_name "subs"}))]
        (is (= "hi-again" (deref @called 1000 :failure)))))))

(deftest listen-with-concurrency
  (with-open [connection (.createConnection default nil)
              queue (create-endpoint "listen-queue")]
    (let [thread-ids (atom #{})
          latch (java.util.concurrent.CountDownLatch. 10)
          listener (.listen connection queue
                     (reify MessageHandler
                       (onMessage [_ msg]
                         (let [msg (read-string (.body msg String))]
                           (Thread/sleep 50)
                           (swap! thread-ids conj (.getId (Thread/currentThread)))
                           (.countDown latch))))
                     (coerce-listen-options {:concurrency 5}))]
      (dotimes [n 10]
        (.send connection queue (str n) nil nil))
      (.await latch)
      (is (= 5 (count @thread-ids)))
      (.close listener))))

(deftest request-response
  (with-open [connection (.createConnection default nil)
              queue (create-endpoint "listen-queue")
              listener (.respond connection queue
                         (reify MessageHandler
                           (onMessage [_ msg]
                             (.body msg String)))
                         nil)]
    (let [response (.request connection queue "hi" nil nil)]
      (is (= "hi" (.body (.get response) String)))
      ;; result should be cached
      (is (= "hi" (.body (.get response) String))))))

(deftest request-response-should-coordinate-requests-with-responses
  (with-open [connection (.createConnection default nil)
              queue (create-endpoint "rr-coord-queue")
              listener (.respond connection queue
                         (reify MessageHandler
                           (onMessage [_ msg]
                             (let [time (read-string (.body msg String))]
                               (Thread/sleep time)
                               (str "response-" time))))
                         (coerce-respond-options {:concurrency 5}))]
    (let [response1 (.request connection queue "50" nil nil)
          response2 (.request connection queue "100" nil nil)
          response3 (.request connection queue "25" nil nil)]
      (is (= "response-50" (.body (.get response1) String)))
      (is (= "response-100" (.body (.get response2) String)))
      (is (= "response-25" (.body (.get response3) String))))))

(deftest request-response-with-ttl
  (with-open [connection (.createConnection default nil)
              queue (create-endpoint "rr-queue")
              listener (.respond connection queue
                     (reify MessageHandler
                       (onMessage [_ msg]
                         (.body msg String)))
                     (coerce-respond-options {:ttl 1}))]
    (let [response (.request connection queue "nope" nil nil)]
      (Thread/sleep 100)
      (is (thrown? java.util.concurrent.TimeoutException
            (.get response 1 TimeUnit/MILLISECONDS))))))
