(ns yorck-ratings.cache
  (:import (java.time LocalDate Clock)))

(def ^:private initial-value {:date  nil
                              :value nil})

(def ^:private cache (atom initial-value))

(def ^:dynamic ^Clock clock (Clock/systemUTC))

(defn- today [] (LocalDate/now clock))

(defn from-cache []
  (println @cache)
  (when (= (:date @cache) (today))
    (:value @cache)))

(defn into-cache [value]
  (swap! cache assoc :value value :date (today))
  value)

(defn reset []
  (reset! cache initial-value))