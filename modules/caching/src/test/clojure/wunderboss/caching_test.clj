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

(ns wunderboss.caching-test
  (:require [clojure.test :refer :all])
  (:import org.projectodd.wunderboss.WunderBoss
           [org.projectodd.wunderboss.caching Caching Caching$CreateOption Config]
           org.projectodd.wunderboss.Options
           org.infinispan.configuration.cache.CacheMode
           java.util.Arrays))

(def default (doto (WunderBoss/findOrCreateComponent Caching) (.start)))

(deftest byte-array-keys
  (let [c (.findOrCreate default "bytes" (Options.))
        k (byte-array [1 2 3])
        v (byte-array [4 5 6])]
    (.put c k v)
    (is (Arrays/equals (byte-array [4 5 6]) (get c (byte-array [1 2 3]))))))

(deftest mode-local-if-not-clustered
  (let [options (Options. {Caching$CreateOption/MODE "repl_sync"})
        config (Config/uration options)
        c (.findOrCreate default "repl" options)]
    (is (= CacheMode/REPL_SYNC (.. config clustering cacheMode)))
    (is (= CacheMode/LOCAL (.. c getCacheConfiguration clustering cacheMode)))))
