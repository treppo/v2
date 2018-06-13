(ns yorck-ratings.rated-movie)

(defrecord RatedMovie [rating rating-count imdb-title imdb-url yorck-title yorck-url])

(def fallback-url "#")

(defn make
  [{:keys [yorck-title rating rating-count imdb-title imdb-url yorck-url]
    :or   {rating       0
           rating-count 0
           imdb-title   "No IMDB title"
           imdb-url     fallback-url
           yorck-url    fallback-url}}]
  (RatedMovie. rating rating-count imdb-title imdb-url yorck-title yorck-url))

(defn has-imdb-infos? [rated-movie]
  (not= fallback-url (:imdb-url rated-movie)))
