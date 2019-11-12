(ns cinema-ratings.rated-movie)

(defn make [{:keys [cinema-info imdb-info]}]
  (assert (not (nil? cinema-info)) "At least cinema info must be provided")
  {:cinema-info cinema-info
   :imdb-info   imdb-info})

(defn from-cinema-info [[title url]]
  (make {:cinema-info {:title title
                       :url   url}}))

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
