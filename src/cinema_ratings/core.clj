(ns cinema-ratings.core
  (:require [cinema-ratings.rated-movie :as rated-movie]
            [cinema-ratings.cache :refer [into-cache from-cache]]
            [clojure.core.async :refer [go chan >! <! close! pipeline-blocking go-loop onto-chan]]))

(defn- join [to f from]
  (go-loop [collection []]
    (if-let [x (<! from)]
      (recur (conj collection x))
      (do
        (>! to (f collection))
        (close! to)))))

(defn rated-movies [cinema-info imdb-search imdb-detail result-chan]
  (if-let [sorted-movies (from-cache)]
    (go (>! result-chan sorted-movies))
    (let [cinema-chan (chan)
          imdb-search-chan (chan)
          imdb-detail-chan (chan)
          concurrency (.availableProcessors (Runtime/getRuntime))]

      (onto-chan cinema-chan (cinema-info))
      (pipeline-blocking concurrency imdb-search-chan (map imdb-search) cinema-chan)
      (pipeline-blocking concurrency imdb-detail-chan (map imdb-detail) imdb-search-chan)
      (join result-chan (fn [movies] (into-cache (rated-movie/sorted movies))) imdb-detail-chan))))