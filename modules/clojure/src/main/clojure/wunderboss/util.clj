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

(ns wunderboss.util
  (:import org.projectodd.wunderboss.WunderBoss))

(defonce ^:private exit-tasks (atom []))

(defn at-exit [f]
  (swap! exit-tasks conj f))

(defn exit! []
  (doseq [f @exit-tasks]
    (f)))

(defn service-registry []
  (get (WunderBoss/options) "service-registry"))

(defn in-container? []
  (not (nil? (service-registry))))

(if-not (in-container?)
  (.addShutdownHook (Runtime/getRuntime) (Thread. exit!)))
