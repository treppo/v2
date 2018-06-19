(ns yorck-ratings.core
  (:require [yorck-ratings.yorck :as yorck]
            [yorck-ratings.imdb :as imdb]
            [yorck-ratings.rated-movie :as rated-movie]
            [clojure.core.async :refer [go chan >! <! close! pipeline-blocking go-loop onto-chan]]))

(defn- join [to f from]
  (go-loop [collection []]
    (if-let [x (<! from)]
      (recur (conj collection x))
      (do
        (>! to (f collection))
        (close! to)))))

(defn rated-movies [result-chan]
  (let [yorck-infos-chan (chan)
        imdb-search-chan (chan)
        imdb-detail-chan (chan)
        concurrency (.availableProcessors (Runtime/getRuntime))]

    (onto-chan yorck-infos-chan (yorck/infos yorck/get-yorck-infos))
    (pipeline-blocking concurrency imdb-search-chan (map (partial imdb/search imdb/get-search-page)) yorck-infos-chan)
    (pipeline-blocking concurrency imdb-detail-chan (map (partial imdb/detail imdb/get-detail-page)) imdb-search-chan)
    (join result-chan rated-movie/sorted imdb-detail-chan)))
