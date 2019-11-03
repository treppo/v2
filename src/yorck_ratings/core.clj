(ns yorck-ratings.core
  (:require [yorck-ratings.yorck :as yorck]
            [yorck-ratings.imdb :as imdb]
            [yorck-ratings.rated-movie :as rated-movie]
            [yorck-ratings.cache :refer [into-cache from-cache]]
            [clojure.core.async :refer [go chan >! <! close! pipeline-blocking go-loop onto-chan]]))

(defn- join [to f from]
  (go-loop [collection []]
    (if-let [x (<! from)]
      (recur (conj collection x))
      (do
        (>! to (f collection))
        (close! to)))))

(defn rated-movies [result-chan]
  (if-let [sorted-movies (from-cache)]
    (go (>! result-chan sorted-movies))
    (let [yorck-chan (chan)
        imdb-search-chan (chan)
        imdb-detail-chan (chan)
        concurrency (.availableProcessors (Runtime/getRuntime))]

    (onto-chan yorck-chan (yorck/info yorck/get-yorck-info))
    (pipeline-blocking concurrency imdb-search-chan (map (partial imdb/search imdb/get-search-page)) yorck-chan)
    (pipeline-blocking concurrency imdb-detail-chan (map (partial imdb/detail imdb/get-detail-page)) imdb-search-chan)
    (join result-chan (fn [movies] (into-cache (rated-movie/sorted movies))) imdb-detail-chan))))
