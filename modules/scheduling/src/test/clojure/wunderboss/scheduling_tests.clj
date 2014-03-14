(ns wunderboss.scheduling-tests
  (:require [clojure.test :refer :all])
  (:import org.projectodd.wunderboss.WunderBoss
           org.projectodd.wunderboss.scheduling.Scheduling
           org.projectodd.wunderboss.scheduling.Scheduling$ScheduleOption
           [java.util Date EnumSet]))

(def default (WunderBoss/findOrCreateComponent Scheduling))

(let [avail-options (->> Scheduling$ScheduleOption
                      EnumSet/allOf
                      (map #(vector (keyword (.value %)) %))
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

(deftest unschedule
  (let [q (atom 0)]
    (with-job #(swap! q inc) {:every 100}
      (Thread/sleep 500)
      (is (> @q 0))
      (.unschedule default "a-job")
      (Thread/sleep 500)
      (let [curr @q]
        (Thread/sleep 500)
        (is (= curr @q))))))

(deftest rescheduling
  (let [q1 (atom 0)
        q2 (atom 0)]
    (with-job #(swap! q1 inc) {:every 100}
      (Thread/sleep 500)
      (is (> @q1 0))
      (with-job #(swap! q2 inc) {:every 100}
        (let [curr1 @q1]
          (Thread/sleep 500)
          (is (> @q2 0))
          (is (= curr1 @q1)))))))

(deftest no-options-should-fire-once
  (let [q (atom 0)]
    (with-job #(swap! q inc) {}
      (Thread/sleep 200)
      (is (= 1 @q)))))

(deftest cron-jobs-should-work
  (let [q (atom 0)]
    (with-job #(swap! q inc) {:cronspec "*/1 * * * * ?"}
      (Thread/sleep 3000)
      (is (> @q 2)))))

(testing "at jobs"
  (deftest in-should-fire-once-in-x-ms
    (let [q (promise)]
      (with-job #(deliver q "ping") {:in 1000}
        (is (nil? (deref q 900 nil)))
        (is (= "ping" (deref q 200 :fail))))))

  (deftest at-should-fire-once-then
    (let [q (promise)]
      (with-job  #(deliver q "ping") {:at (Date. (+ 1000 (System/currentTimeMillis)))} 
        (is (nil? (deref q 900 nil)))
        (is (= "ping" (deref q 200 :fail))))))

  (deftest every-should-fire-immediately-and-continuously
    (let [q (atom 0)]
      (with-job #(swap! q inc) {:every 100}
        (dotimes [i 5]
          (Thread/sleep (if (zero? i) 20 100))
          (is (= (inc i) @q))))))

  (deftest every-with-repeat-should-fire-immediately-x-times
    (let [q (atom 0)]
      (with-job #(swap! q inc) {:every 100 :repeat 1}
        (Thread/sleep 20)
        (is (= 1 @q))
        (dotimes [_ 2]
          (Thread/sleep 100)
          (is (= 2 @q))))))

  (deftest at-with-every-should-fire-immediately-and-continuously-starting-at-at
    (let [q (atom 0)]
      (with-job #(swap! q inc) {:at (Date. (+ 1000 (System/currentTimeMillis)))
                                :every 100}
        (Thread/sleep 500)
        (= (zero? @q))
        (dotimes [i 5]
          (Thread/sleep (if (zero? i) 520 100))
          (is (= (inc i) @q))))))

  (deftest until-with-every-should-repeat-until-until
    (let [q (atom 0)
          step 222]
      (with-job #(swap! q inc) {:until (Date. (+ 1000 (System/currentTimeMillis)))
                                :every step}
        (dotimes [i 5]
          (Thread/sleep (if (zero? i) 20 step))
          (is (= (inc i) @q)))
        (Thread/sleep step)
        (is (= 5 @q)))))

  (deftest at-with-in-should-throw
    (is (thrown?
          IllegalArgumentException
          (with-job* (fn []) {:at 5 :in 5} (fn [])))))  

  (deftest repeat-without-every-should-throw
    (is (thrown?
          IllegalArgumentException
          (with-job* (fn []) {:repeat 5} (fn [])))))

  (deftest until-without-every-should-throw
    (is (thrown?
          IllegalArgumentException
          (with-job* (fn []) {:until 5} (fn []))))))

