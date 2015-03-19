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

(ns wunderboss.scheduling-tests
  (:require [clojure.test :refer :all])
  (:import [org.projectodd.wunderboss Option WunderBoss]
           [org.projectodd.wunderboss.scheduling Scheduling Scheduling$ScheduleOption]
           org.quartz.listeners.TriggerListenerSupport
           org.quartz.TriggerUtils
           java.util.Date))

(def default (WunderBoss/findOrCreateComponent Scheduling))

(let [avail-options (->> Scheduling$ScheduleOption
                      Option/optsFor
                      (map #(vector (keyword (.name %)) %))
                      (into {}))]
  (defn coerce-options [opts]
    (reduce (fn [m [k v]]
              (assoc m
                (if-let [enum (avail-options k)]
                  enum
                  (throw (IllegalArgumentException. (str k " is not a valid option."))))
                v))
      {}
      opts)))

(defn with-job*
  ([job options f]
     (with-job* default "a-job" job options f))
  ([scheduler name job options f]
     (try
       (.schedule scheduler name job (coerce-options options))
       (f)
       (finally
         (.unschedule scheduler name)))))

(defmacro with-job [job options & body]
  `(with-job* ~job ~options (fn [] ~@body)))

(defn trigger-for-job
  ([]
     (trigger-for-job default "a-job"))
  ([scheduler name]
     (-> (.scheduler scheduler)
       (.getTriggersOfJob (.lookupJob scheduler name))
       first)))

(defn fire-times-for-job
  ([count]
     (fire-times-for-job default "a-job" count))
  ([scheduler name count]
     (TriggerUtils/computeFireTimes (trigger-for-job scheduler name), nil, count)))

(deftest multiple-schedulers
  (let [other-scheduler (WunderBoss/findOrCreateComponent Scheduling "other" nil)]
    (is (not= other-scheduler default))
    (.start other-scheduler)
    (is (not= (.scheduler other-scheduler) (.scheduler default)))))

(deftest unschedule
  (let [started? (promise)
        should-run? (atom true)]
    (with-job
      (fn []
        (is @should-run?)
        (deliver started? true))
      {:every 100}
      (is (deref started? 5000 false))
      (is (.unschedule default "a-job"))
      (is (not (.unschedule default "a-job")))
      (reset! should-run? false))))

(deftest scheduledJobs
  (with-job #() {:every 100}
    (let [jobs (.scheduledJobs default)]
      (is (= #{"a-job"} jobs))
      ;; it should be unmodifiable
      (is (thrown? UnsupportedOperationException
            (.add jobs "should-fail"))))))

(deftest rescheduling
  (let [started1? (promise)
        should-run1? (atom true)
        started2? (promise)]
    (with-job
      (fn []
        (is @should-run1?)
        (deliver started1? true))
      {:every 100}
      (is (deref started1? 5000 false))
      (with-job #(deliver started2? true) {:every 100}
        (reset! should-run1? false)
        (is (deref started2? 5000 false))))))

(deftest no-options-should-fire-once
  (let [started? (promise)]
    (with-job
      (fn []
        (is (not (realized? started?)))
        (deliver started? true))
      {}
      (is (deref started? 5000 false)))))

(testing "cron jobs"
  (deftest cron-jobs-should-work
    (with-job #() {:cron "*/1 * * * * ?"}
      (doseq [[t1 t2] (partition 2 1 (fire-times-for-job 3))]
        (is (= 1000 (- (.getTime t2) (.getTime t1)))))))

  (deftest in-with-cron-should-should-start-near-x-ms
    (let [now (System/currentTimeMillis)]
      (with-job #() {:in 2000
                     :cron "*/1 * * * * ?"}
        (let [trigger-start (.. (trigger-for-job) getStartTime getTime)]
          (is (<= 1000 (- trigger-start now)))
          (is (>= 2000 (- trigger-start now)))
          (doseq [[t1 t2] (partition 2 1 (fire-times-for-job 3))]
            (is (= 1000 (- (.getTime t2) (.getTime t1)))))))))

  (deftest at-with-cron-should-should-start-near-x
    (let [start (+ 2000 (System/currentTimeMillis))]
      (with-job #() {:at (Date. start)
                     :cron "*/1 * * * * ?"}
        (let [trigger-start (.. (trigger-for-job) getStartTime getTime)]
          (is (< (- start trigger-start) 1000))))))

  (deftest until-with-cron-should-run-until
    (let [end (+ 2000 (System/currentTimeMillis))]
      (with-job #() {:until (Date. end)
                     :cron "*/1 * * * * ?"}
        (is (= end (.getTime (.getEndTime (trigger-for-job)))))))))

(testing "at jobs"
  (deftest in-should-fire-once-in-x-ms
    (with-job #() {:in 30000}
      (let [fire-times (fire-times-for-job 3)
            now (System/currentTimeMillis),
            first-fire (.getTime (first fire-times))]
        (is (<= 29000 (- first-fire now)))
        (is (>= 30000 (- first-fire now)))
        (is (= 1 (count fire-times))))))

  (deftest at-should-fire-once-then
    (let [start (+ 30000 (System/currentTimeMillis))]
      (with-job  #() {:at (Date. start)}
        (let [fire-times (fire-times-for-job 3)
              first-fire (.getTime (first fire-times))]
          (is (>= 1000 (- first-fire start)))
          (is (= 1 (count fire-times)))))))

  (deftest every-should-fire-immediately-and-continuously
    (with-job #() {:every 100}
      (let [fire-times (fire-times-for-job 10)
            trigger-end (.getEndTime (trigger-for-job))]
        (doseq [[t1 t2] (partition 2 1 fire-times)]
          (is (= 100 (- (.getTime t2) (.getTime t1)))))
        (is (= 10 (count fire-times)))
        (is (= nil trigger-end)))))

  (deftest every-with-limit-should-fire-immediately-x-times
    (let [now (System/currentTimeMillis)]
      (with-job #() {:every 100 :limit 2}
        (let [trigger (trigger-for-job)]
          (is (= 1 (.getRepeatCount trigger)))
          (is (= 100 (.getRepeatInterval trigger)))))))

  (deftest at-with-every-should-fire-immediately-and-continuously-starting-at-at
    (let [start (+ 30000 (System/currentTimeMillis))]
      (with-job #() {:at (Date. start)
                     :every 100}
        (let [fire-times (fire-times-for-job 10)
              first-fire (.getTime (first fire-times))
              trigger-end (.getEndTime (trigger-for-job))]
          (is (= 10 (count fire-times)))
          (is (>= 1000 (- first-fire start)))
          (is (= nil trigger-end))))))

  (deftest short-until-overrides-limit
    (with-job #() {:until (Date. (+ 31000 (System/currentTimeMillis)))
                   :in 30000
                   :every 222
                   :limit 9999}
      (is (= 5 (count (fire-times-for-job 10))))))

  (deftest short-limit-overrides-until
    (with-job #() {:until (Date. (+ 31000 (System/currentTimeMillis)))
                   :in 30000
                   :every 222
                   :limit 1}
      (is (= 1 (count (fire-times-for-job 10))))))

  (deftest until-with-every-should-repeat-until-until
    (let [end (+ 31000 (System/currentTimeMillis))]
      (with-job #() {:until (Date. end)
                     :in 30000
                     :every 222}
        (is (= 5 (count (fire-times-for-job 10))))
        (is (= end (.. (trigger-for-job) getEndTime getTime))))))

  (deftest at-with-in-should-throw
    (is (thrown?
          IllegalArgumentException
          (with-job* (fn []) {:at 5 :in 5} (fn [])))))

  (deftest limit-without-every-should-throw
    (is (thrown?
          IllegalArgumentException
          (with-job* (fn []) {:limit 5} (fn [])))))

  (deftest until-without-every-or-cron-should-throw
    (is (thrown?
          IllegalArgumentException
          (with-job* (fn []) {:until 5} (fn []))))))

(deftest completed-jobs-should-auto-unschedule
  (is (empty? (.scheduledJobs default)))
  (let [p (promise)
        id "auto-unschedule"]
    (-> (.scheduler default)
      .getListenerManager
      (.addTriggerListener (proxy [TriggerListenerSupport] []
                             (getName [] id)
                             (triggerComplete [_ _ _]
                               (deliver p :success)))))
    (.schedule default id #() {})
    (is (= :success (deref p 1000 :failure)))
    (is (false? (.unschedule default id)))
    (is (empty? (.scheduledJobs default)))
    (-> (.scheduler default)
      .getListenerManager
      (.removeTriggerListener id))))
