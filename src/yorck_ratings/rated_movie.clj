(ns yorck-ratings.rated-movie)

(defrecord RatedMovie [rating rating-count imdb-title imdb-url yorck-title yorck-url])

(defn make
  [{:keys [yorck-title rating rating-count imdb-title imdb-url yorck-url]
    :or   {rating       0
           rating-count 0
           imdb-title   "No title"
           imdb-url     ""
           yorck-url    ""}}]
  (RatedMovie. rating rating-count imdb-title imdb-url yorck-title yorck-url))
