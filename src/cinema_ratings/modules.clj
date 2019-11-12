(ns cinema-ratings.modules
  (:require [cinema-ratings.imdb :as imdb]
            [cinema-ratings.yorck :as cinema]
            [cinema-ratings.core :as core]))

(def rated-movies
  (let [cinema-info (partial cinema/info cinema/get-cinema-info)
        imdb-search (partial imdb/search imdb/get-search-info)
        imdb-detail (partial imdb/detail imdb/get-detail-info)]
    (partial core/rated-movies cinema-info imdb-search imdb-detail)))