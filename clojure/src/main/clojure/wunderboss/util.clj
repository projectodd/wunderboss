;; Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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
  (:import [org.projectodd.wunderboss
            DynamicClassLoader WunderBoss]))

(defn at-exit [f]
  (WunderBoss/addShutdownAction f))

(defn options []
  (WunderBoss/options))

(defn service-registry []
  (get (options) "service-registry"))

(try
  (require '[dynapath.dynamic-classpath :as dp])
  (eval '(let [base-url-classloader
               (assoc dp/base-readable-addable-classpath
                 :classpath-urls (fn [^DynamicClassLoader cl]
                                   (seq (.getURLs cl)))
                 :add-classpath-url (fn [^DynamicClassLoader cl url]
                                      (.addURL cl url)))]

           ;; if dynapath is available, make our classloader join the party
           (extend org.projectodd.wunderboss.DynamicClassLoader
             dp/DynamicClasspath
             base-url-classloader)

           ;; users of dynapath often search for the highest addable loader,
           ;; which in the container will be the AppClassLoader, and we really
           ;; want the DynamicClassLoader to be used instead. Anything added
           ;; to the AppClassLoader won't be seen, since JBoss Modules is
           ;; between the ACL and the app.
           (when (WunderBoss/inContainer)
             (extend sun.misc.Launcher$AppClassLoader
               dp/DynamicClasspath
               (assoc base-url-classloader
                 :can-add? (constantly false))))))
  (catch Exception _))
