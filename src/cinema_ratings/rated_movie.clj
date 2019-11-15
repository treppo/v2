(ns cinema-ratings.rated-movie
  (:require [clojure.spec.alpha :as spec])
  (:import (java.net URL)))

(defn- url? [s]
  (try
    (boolean (URL. s))
    (catch Exception _ false)))

(spec/def ::title string?)
(spec/def ::url url?)
(spec/def ::rating float?)
(spec/def ::ratings-count int?)
(spec/def ::cinema-info (spec/keys :req-un [::title ::url]))
(spec/def ::imdb-info (spec/keys :req-un [::title ::url]
                                 :opt-un [::rating ::ratings-count]))
(spec/def ::rated-movie (spec/keys :req-un [::cinema-info ::imdb-info]))

(defn- make [{:keys [cinema-info imdb-info]}]
  {:cinema-info cinema-info
   :imdb-info   imdb-info})

(defn from-cinema-info [cinema-info]
  (make {:cinema-info cinema-info}))

(spec/fdef from-cinema-info
  :args (spec/cat :cinema-info ::cinema-info)
  :ret ::rated-movie)

(defn with-imdb-info [rated-movie [title url]]
  (merge rated-movie {:imdb-info {:title title
                                  :url   url}}))

(defn with-imdb-rating [rated-movie [rating rating-count]]
  (merge-with into rated-movie {:imdb-info {:rating       rating
                                            :rating-count rating-count}}))

(defn rating [rated-movie]
  (get-in rated-movie [:imdb-info :rating]))

(defn rating-count [rated-movie]
  (get-in rated-movie [:imdb-info :rating-count]))

(defn cinema-title [rated-movie]
  (get-in rated-movie [:cinema-info :title]))

(defn cinema-url [rated-movie]
  (get-in rated-movie [:cinema-info :url]))

(defn imdb-title [rated-movie]
  (get-in rated-movie [:imdb-info :title]))

(defn imdb-url [rated-movie]
  (get-in rated-movie [:imdb-info :url]))

(defn has-imdb-info? [rated-movie]
  (:imdb-info rated-movie))

(defn has-imdb-rating? [rated-movie]
  (rating rated-movie))

(defn no-imdb-rating? [rated-movie]
  (not (has-imdb-rating? rated-movie)))

(def ^:private rating-threshold 7)
(def ^:private count-threshold 1000)

(defn- count-above-threshold [rated-movie]
  (>= (rating-count rated-movie) count-threshold))

(defn- count-below-threshold [rated-movie]
  (< (rating-count rated-movie) count-threshold))

(defn- rating-above-threshold [rated-movie]
  (>= (rating rated-movie) rating-threshold))

(defn- rating-below-threshold [rated-movie]
  (< (rating rated-movie) rating-threshold))

(defn hot? [rated-movie]
  (and (has-imdb-rating? rated-movie) (rating-above-threshold rated-movie) (count-above-threshold rated-movie)))

(defn considerable? [rated-movie]
  (or
   (no-imdb-rating? rated-movie)
   (and (rating-above-threshold rated-movie)
        (count-below-threshold rated-movie))))

(defn- by-rating [a b]
  (cond
    (and (no-imdb-rating? a) (no-imdb-rating? b)) 0
    (and (no-imdb-rating? a) (rating-above-threshold b)) -1
    (and (no-imdb-rating? a) (rating-below-threshold b)) 1
    (and (rating-below-threshold a) (no-imdb-rating? b)) -1
    :else (compare (rating a) (rating b))))

(defn sorted [rated-movies]
  (reverse (sort by-rating rated-movies)))
