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
  (:import org.projectodd.wunderboss.WunderBoss
           org.projectodd.wunderboss.messaging.Messaging
           org.projectodd.wunderboss.messaging.Messaging$CreateQueueOption
           org.projectodd.wunderboss.messaging.Messaging$ListenOption
           java.util.EnumSet
           javax.jms.Session))

(def default (doto (WunderBoss/findOrCreateComponent Messaging) (.start)))

(defn create-opts-fn [enum]
  (let [avail-options (->> enum
                        EnumSet/allOf
                        (map #(vector (keyword (.value %)) %))
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

(def coerce-listen-options (create-opts-fn Messaging$ListenOption))

(deftest queue-creation-publish-receive-release
  (with-open [connection (doto (.createConnection default nil) (.start))]
    (let [queue (.findOrCreateQueue default "a-queue" nil)
          session (.createSession connection false Session/AUTO_ACKNOWLEDGE)
          producer (.createProducer session queue)
          consumer (.createConsumer session queue)]

      ;; findOrCreateQueue should return the same queue for the same name
      (is (= queue (.findOrCreateQueue default "a-queue" nil)))

      ;; we should be able to send and rcv
      (.send producer (.createTextMessage session "hi"))
      (is (= "hi" (.getText (.receive consumer 100))))

      ;; a released queue should no longer be avaiable
      (.releaseQueue default "a-queue" true)
      (.send producer (.createTextMessage session "hi"))
      (is (thrown? javax.jms.IllegalStateException
            (.receive consumer 100))))))

(deftest listen
  (with-open [connection (doto (.createConnection default nil) (.start))]
    (let [queue (.findOrCreateQueue default "listen-queue" nil)
          session (.createSession connection false Session/AUTO_ACKNOWLEDGE)
          producer (.createProducer session queue)
          called (atom (promise))
          id (.listen default queue
               (reify javax.jms.MessageListener
                 (onMessage [_ msg]
                   (deliver @called (.getText msg))))
               nil)]
      (.send producer (.createTextMessage session "hi"))
      (is (= "hi" (deref @called 1000 :failure)))

      (reset! called (promise))
      (.unlisten default id)
      (.send producer (.createTextMessage session "hi"))
      (is (= :success (deref @called 1000 :success))))))
