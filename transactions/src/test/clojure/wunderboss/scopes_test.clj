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

(ns wunderboss.scopes-test
  (:require [clojure.test :refer :all])
  (:import [org.projectodd.wunderboss WunderBoss Options]
           [org.projectodd.wunderboss.caching Caching Caching$CreateOption]
           [org.projectodd.wunderboss.transactions Transaction]))

(def tx (doto (WunderBoss/findOrCreateComponent Transaction) (.start)))

;;; Create a cache for testing transactional scope behavior
(def cache (let [service (doto (WunderBoss/findOrCreateComponent Caching) (.start))
                 options (Options. {Caching$CreateOption/TRANSACTIONAL true})]
             (.findOrCreate service "tx-test" options)))

;;; Clear cache before each test
(use-fixtures :each (fn [f] (.clear cache) (f)))

(defmacro catch-exception [& body]
  `(try ~@body (catch Exception _#)))

(deftest required-commit
  (.required tx
    (fn []
      (.put cache :a 1)
      (.required tx #(.put cache :b 2))))
  (is (= 1 (:a cache)))
  (is (= 2 (:b cache))))

(deftest required-rollback-parent
  (catch-exception
    (.required tx
      (fn []
        (.put cache :a 1)
        (.required tx #(.put cache :b 2))
        (throw (Exception. "force rollback")))))
  (is (empty? cache)))

(deftest required-rollback-parent-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.required tx #(.put cache :b 2))
      (-> tx .manager .setRollbackOnly)))
  (is (empty? cache)))

(deftest required-rollback-child
  (catch-exception
    (.required tx
      (fn []
        (.put cache :a 1)
        (.required tx
          (fn []
            (.put cache :b 2)
            (throw (Exception. "force rollback")))))))
  (is (empty? cache)))

(deftest required-rollback-child-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.required tx
        (fn []
          (.put cache :b 2)
          (-> tx .manager .setRollbackOnly)))))
  (is (empty? cache)))

(deftest requires-new-commit
  (.required tx
    (fn []
      (.put cache :a 1)
      (.requiresNew tx #(.put cache :b 2))))
  (is (= 1 (:a cache)))
  (is (= 2 (:b cache))))

(deftest requires-new-rollback-parent
  (catch-exception
    (.required tx
      (fn []
        (.put cache :a 1)
        (.requiresNew tx #(.put cache :b 2))
        (throw (Exception. "force rollback")))))
  (is (nil? (:a cache)))
  (is (= 2 (:b cache))))

(deftest requires-new-rollback-parent-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.requiresNew tx #(.put cache :b 2))
      (-> tx .manager .setRollbackOnly)))
  (is (nil? (:a cache)))
  (is (= 2 (:b cache))))

(deftest requires-new-rollback-child
  (.required tx
    (fn []
      (.put cache :a 1)
      (catch-exception
        (.requiresNew tx
          (fn []
            (.put cache :b 2)
            (throw (Exception. "force rollback")))))))
  (is (= 1 (:a cache)))
  (is (nil? (:b cache))))

(deftest requires-new-rollback-child-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.requiresNew tx
        (fn []
          (.put cache :b 2)
          (-> tx .manager .setRollbackOnly)))))
  (is (= 1 (:a cache)))
  (is (nil? (:b cache))))

(deftest mandatory-commit
  (.required tx
    (fn []
      (.put cache :a 1)
      (.mandatory tx #(.put cache :b 2))))
  (is (= 1 (:a cache)))
  (is (= 2 (:b cache))))

(deftest mandatory-rollback
  (is (thrown? Exception
        (.put cache :a 1)
        (.mandatory tx #(.put cache :b 2))))
  (is (= 1 (:a cache)))
  (is (nil? (:b cache))))

(deftest never-saves
  (.put cache :a 1)
  (.never tx #(.put cache :b 2))
  (is (= 1 (:a cache)))
  (is (= 2 (:b cache))))

(deftest never-rollback
  (is (thrown? Exception
        (.required tx
          (fn []
            (.put cache :a 1)
            (.never tx #(.put cache :b 2))))))
  (is (empty? cache)))

(deftest not-supported-rollback-parent
  (catch-exception
    (.required tx
      (fn []
        (.put cache :a 1)
        (.notSupported tx #(.put cache :b 2))
        (throw (Exception. "force rollback")))))
  (is (nil? (:a cache)))
  (is (= 2 (:b cache))))

(deftest not-supported-rollback-parent-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.notSupported tx #(.put cache :b 2))
      (-> tx .manager .setRollbackOnly)))
  (is (nil? (:a cache)))
  (is (= 2 (:b cache))))

(deftest not-supported-rollback-child
  (.required tx
    (fn []
      (.put cache :a 1)
      (catch-exception
        (.notSupported tx
          (fn []
            (.put cache :b 2)
            (throw (Exception. "force rollback")))))))
  (is (= 1 (:a cache)))
  (is (= 2 (:b cache))))

(deftest supports-saves
  (catch-exception
    (.supports tx
      (fn []
        (.put cache :a 1)
        (throw (Exception. "force rollback")))))
  (is (= 1 (:a cache))))

(deftest supports-rollback-parent
  (catch-exception
    (.required tx
      (fn []
        (.put cache :a 1)
        (.supports tx #(.put cache :b 2))
        (throw (Exception. "force rollback")))))
  (is (empty? cache)))

(deftest supports-rollback-parent-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.supports tx #(.put cache :b 2))
      (-> tx .manager .setRollbackOnly)))
  (is (empty? cache)))

(deftest supports-rollback-child
  (catch-exception
    (.required tx
      (fn []
        (.put cache :a 1)
        (.supports tx
          (fn []
            (.put cache :b 2)
            (throw (Exception. "force rollback")))))))
  (is (empty? cache)))

(deftest supports-rollback-child-sro
  (.required tx
    (fn []
      (.put cache :a 1)
      (.supports tx
        (fn []
          (.put cache :b 2)
          (-> tx .manager .setRollbackOnly)))))
  (is (empty? cache)))
