(ns yorck-ratings.core
  (:require [org.httpkit.client :as http]))

(defrecord RatedMovie [rating imdb-title yorck-title])

(defn rated-movies []
  [(RatedMovie. 8 "The Hateful Eight (2015)" "The Hateful Eight")])