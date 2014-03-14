(ns build.calc_classpaths
  (:require [leiningen.core.classpath :as cp]
            [leiningen.core.project :as project]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(let [[app-dir dest-dir] *command-line-args*]
  (println "Calculating lein classpath for" app-dir)
  (spit
    (io/file
      (doto (io/file dest-dir)
        (.mkdirs))
      "lein-classpath")
    (str/join ":"
      (-> app-dir
        (str "/project.clj")
        project/read
        cp/get-classpath))))
