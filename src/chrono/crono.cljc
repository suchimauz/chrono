(ns chrono.crono
  (:require [chrono.core :as ch]
            [chrono.now :as now]
            [chrono.calendar :as cal]))

(def needed-for
  {:month [:yaer :month]
   :day [:year :month :day]
   :hour [:year :month :day :hour]
   :min [:year :month :day :hour :min]
   :sec [:year :month :day :hour :min :sec]})

(def default-at
  {:hour {:min 0}
   :min {:sec 0}})

(defn next-time-assumption [current-time {every :every at :at}])

(def days-of-week [:sunday :monday :tuesday :wednesday :thursday :friday :saturday])

(defn nearest-week-day [current-time day-of-week]
  (first
   (filter
    #(= (.indexOf days-of-week day-of-week)
        (cal/day-of-week (:year %) (:month %) (:day %)))
    (map
     #(ch/+ current-time {:day %})
     (range 7)))))

(defn every-month? [every]
  (contains? (set days-of-week) every))

(defn next-time
  ([cfg] (next-time (now/utc) cfg))
  ([current-time {every :every at :at :as when}]
   (let [every (keyword every)
         at (or at (get default-at every))
         _ (if (nil? every) (throw (ex-info ":every must be specified" {:when when})))
         _ (if (nil? at) (throw (ex-info ":at must be specified" {:when when})))
         at (if (map? at) [at] at)
         current-time (if (every-month? every) (nearest-week-day current-time every) current-time)
         assumptions (map #(merge (select-keys current-time (get needed-for (if (every-month? every) :day every))) %) at)]
     (if (nil? (first (filter #(ch/< current-time %) assumptions)))
       (ch/+ (first assumptions) (if (every-month? every) {:day 7} {every 1}))
       (first (filter #(ch/< current-time %) assumptions))))))

(defn now?
  ([cfg] (now? (now/utc) cfg))
  ([current-time {every :every until :until :as when}]
   (if until
     (let [utmost-time (merge (select-keys current-time (get needed-for every)) until)]
       (ch/< current-time utmost-time))
     true)))

(comment

  (= {:year 2020 :month 1 :day 1 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 11}
                {:every "day" :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 2 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 12 :min 10}
                {:every :day :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 1 :hour 14}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 13}
                  {:every :day :at [{:hour 12}
                                    {:hour 14}]}))

  (= {:year 2020 :month 1 :day 1 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 11}
                {:every :day :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 2 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 12 :min 10}
                {:every :day :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 1 :hour 14}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 13}
                  {:every :day :at [{:hour 12}
                                    {:hour 14}]}))

  (= {:year 2020 :month 1 :day 1 :hour 10 :min 30}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 10 :min 13}
                  {:every :hour :at [{:min 0} {:min 30}]}))

  (= {:year 2020 :month 1 :day 1 :hour 12}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 11 :min 43}
                  {:every :hour :at [{:min 0} {:min 30}]}))

  (= true
     (now? {:year 2020 :month 1 :day 1 :hour 12 :min 31}
           {:every :day
            :at {:hour 12}
            :until {:hour 12 :min 30}}))

  )
