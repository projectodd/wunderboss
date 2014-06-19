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

(ns wunderboss.web-test
  (:require [clojure.test :refer :all])
  (:import org.projectodd.wunderboss.WunderBoss
           org.projectodd.wunderboss.Options
           io.undertow.server.HttpHandler
           [org.projectodd.wunderboss.web Web UndertowWeb UndertowWeb$Pathology]))

(def default (doto (WunderBoss/findOrCreateComponent Web) (.start)))

(deftest pathology-epilogue
  (let [done (atom false)
        p (UndertowWeb$Pathology.)
        h (reify HttpHandler)
        r #(reset! done true)]
    (.epilogue p h r)
    (.add p "/" ["foo" "bar"] h)
    (is (not @done))
    (.remove p "/" ["bar"])
    (is (not @done))
    (.remove p "/" ["foo"])
    (is @done)))
