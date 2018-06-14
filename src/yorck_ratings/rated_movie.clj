(ns yorck-ratings.rated-movie)

(defn make [{:keys [yorck-info imdb-info imdb-rating]}]
  (assert (not (nil? yorck-info)) "At least Yorck info must be provided")
  {:yorck-info  yorck-info
   :imdb-info   imdb-info
   :imdb-rating imdb-rating})

(defn has-imdb-info? [rated-movie]
  (:imdb-info rated-movie))

(defn has-imdb-rating? [rated-movie]
  (:imdb-rating rated-movie))

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

(defn rating-above [rated-movie threshold]
  (> (rating rated-movie) threshold))

(defn is-considerable-movie? [rated-movie]
  (and (has-imdb-rating? rated-movie) (rating-above rated-movie 7)))

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
