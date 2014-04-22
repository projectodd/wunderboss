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
            Connection Connection$ListenOption Connection$ReceiveOption Connection$SendOption
            MessageHandler]
           java.util.concurrent.TimeUnit))

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

(def coerce-listen-options (create-opts-fn Connection$ListenOption))
(def coerce-receive-options (create-opts-fn Connection$ReceiveOption))
(def coerce-send-options (create-opts-fn Connection$SendOption))

(deftest queue-creation-publish-receive-close
  (with-open [connection (.createConnection default nil)
              endpoint (.findOrCreateEndpoint default "a-queue" nil)]

    ;; findOrCreateEndpoint should return the same queue for the same name
    (is (= (.implementation endpoint)
          (.implementation (.findOrCreateEndpoint default "a-queue" nil))))

    ;; we should be able to send and rcv
    (.send connection endpoint "hi" nil nil)
    (let [msg (.receive connection endpoint (coerce-receive-options {:timeout 1000}))]
      (is msg)
      (is (= "hi" (.body msg String))))

    ;; a closed endpoint should no longer be avaiable
    (.close endpoint)
    (is (thrown? javax.jms.InvalidDestinationException
          (.receive connection endpoint (coerce-receive-options {:timeout 1}))))))

(deftest listen
  (with-open [connection (.createConnection default nil)
              queue (.findOrCreateEndpoint default "listen-queue" nil)]
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

(deftest request-response
  (with-open [connection (.createConnection default nil)
              queue (.findOrCreateEndpoint default "listen-queue" nil)
              listener (.respond connection queue
                         (reify MessageHandler
                           (onMessage [_ msg]
                             (.reply msg (.body msg String) nil nil)))
                         nil)]
    (let [response (.request connection queue "hi" nil nil)]
      (is (= "hi" (.body (.get response) String)))
      ;; result should be cached
      (is (= "hi" (.body (.get response) String))))))

(deftest request-response-with-ttl
  (with-open [connection (.createConnection default nil)
              queue (.findOrCreateEndpoint default "rr-queue" nil)
              listener (.respond connection queue
                     (reify MessageHandler
                       (onMessage [_ msg]
                         (.reply msg (.body msg String) nil
                           (coerce-send-options {:ttl 1}))))
                     nil)]
    (let [response (.request connection queue "nope" nil nil)]
      (Thread/sleep 100)
      (is (nil? (.get response 1 TimeUnit/MILLISECONDS))))))
