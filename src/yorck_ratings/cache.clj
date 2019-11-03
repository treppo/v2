(ns yorck-ratings.cache)

(def ^:private initial-value {:date  nil
                              :value nil})

(def ^:private cache (atom initial-value))

(defn from-cache []
  (:value @cache))

(defn into-cache [value]
  (swap! cache assoc :value value)
  value)

(defn reset []
  (reset! cache initial-value))