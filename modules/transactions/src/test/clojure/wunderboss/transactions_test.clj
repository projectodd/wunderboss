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

(ns wunderboss.transactions-test
  (:require [clojure.test :refer :all])
  (:import [org.projectodd.wunderboss WunderBoss Options]
           [org.projectodd.wunderboss.caching Caching Caching$CreateOption]
           [org.projectodd.wunderboss.transactions Transaction]
           [org.projectodd.wunderboss.codecs Codecs None]
           [org.projectodd.wunderboss.messaging Messaging Context
            Destination$ReceiveOption Destination$MessageOpOption
            Messaging$CreateContextOption Messaging$CreateQueueOption]))

(def tx (doto (WunderBoss/findOrCreateComponent Transaction) (.start)))
(def msg (doto (WunderBoss/findOrCreateComponent Messaging) (.start)))

(def cache (let [service (doto (WunderBoss/findOrCreateComponent Caching) (.start))
                 options (Options. {Caching$CreateOption/TRANSACTIONAL true})]
             (.findOrCreate service "tx-test" options)))

(def queue (let [options (Options. {Messaging$CreateQueueOption/DURABLE false})]
             (.findOrCreateQueue msg "/queue/test" options)))

(def codecs (-> (Codecs.) (.add None/INSTANCE)))

;;; Clear cache before each test
(use-fixtures :each (fn [f] (.clear cache) (f)))

(defn attempt-transaction-external [& [f]]
  (try
    (with-open [context (.createContext msg (Options. {Messaging$CreateContextOption/XA true}))]
      (.required tx
        (fn []
          (.send queue "kiwi" None/INSTANCE (Options. {Destination$MessageOpOption/CONTEXT context}))
          (.put cache :a 1)
          (if f (f)))))
    (catch Exception e
      (-> e .getCause .getMessage))))

(defn attempt-transaction-internal [& [f]]
  (try
    (.required tx
      (fn []
        (.send queue "kiwi" None/INSTANCE (Options. {Destination$MessageOpOption/CONTEXT Context/XA}))
        (.put cache :a 1)
        (if f (f))))
    (catch Exception e
      (-> e .getCause .getMessage))))

(deftest verify-transaction-success-external
  (is (nil? (attempt-transaction-external)))
  (is (= "kiwi" (.body (.receive queue codecs (Options. {Destination$ReceiveOption/TIMEOUT 1000})))))
  (is (= 1 (:a cache))))

(deftest verify-transaction-failure-external
  (is (= "force rollback" (attempt-transaction-external #(throw (Exception. "force rollback")))))
  (is (nil? (.receive queue codecs (Options. {Destination$ReceiveOption/TIMEOUT 1000}))))
  (is (nil? (:a cache))))

(deftest verify-transaction-success-internal
  (is (nil? (attempt-transaction-internal)))
  (is (= "kiwi" (.body (.receive queue codecs (Options. {Destination$ReceiveOption/TIMEOUT 1000})))))
  (is (= 1 (:a cache))))

(deftest verify-transaction-failure-internal
  (is (= "force rollback" (attempt-transaction-internal #(throw (Exception. "force rollback")))))
  (is (nil? (.receive queue codecs (Options. {Destination$ReceiveOption/TIMEOUT 1000}))))
  (is (nil? (:a cache))))
