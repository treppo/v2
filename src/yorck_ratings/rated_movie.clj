(ns yorck-ratings.rated-movie)

(defn make [{:keys [yorck-info imdb-info imdb-rating]}]
  (assert (not (nil? yorck-info)) "At least Yorck info must be provided")
  {:yorck-info  yorck-info
   :imdb-info   imdb-info
   :imdb-rating imdb-rating})

(defn from-yorck-info [[title url]]
  (make {:yorck-info [title url]}))

(defn with-imdb-info [rated-movie [title url]]
  (merge rated-movie {:imdb-info [title url]}))

(defn with-imdb-rating [rated-movie [rating rating-count]]
  (merge rated-movie {:imdb-rating [rating rating-count]}))

(defn rating [rated-movie]
  (let [[rating count] (:imdb-rating rated-movie)]
    rating))

(defn rating-count [rated-movie]
  (let [[rating count] (:imdb-rating rated-movie)]
    count))

(defn yorck-title [rated-movie]
  (let [[title url] (:yorck-info rated-movie)]
    title))

(defn yorck-url [rated-movie]
  (let [[title url] (:yorck-info rated-movie)]
    url))

(defn imdb-title [rated-movie]
  (let [[title url] (:imdb-info rated-movie)]
    title))

(defn imdb-url [rated-movie]
  (let [[title url] (:imdb-info rated-movie)]
    url))

(defn has-imdb-info? [rated-movie]
  (:imdb-info rated-movie))

(defn has-imdb-rating? [rated-movie]
  (:imdb-rating rated-movie))

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
