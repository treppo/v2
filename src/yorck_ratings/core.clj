(ns yorck-ratings.core
  (:require [yorck-ratings.yorck :as yorck]
            [yorck-ratings.imdb :as imdb]
            [yorck-ratings.rated-movie :as rated-movie]
            [clojure.core.async :refer [go chan >! <! close! pipeline-async go-loop onto-chan]]))

(defn- sort-by-rating [movies]
  (reverse (sort-by rated-movie/rating movies)))

(defn rated-movies [result-chan]
  (let [yorck-infos-chan (chan)
        yorck-infos-split-chan (chan)
        imdb-search-chan (chan)
        imdb-detail-chan (chan)
        concurrency (.availableProcessors (Runtime/getRuntime))]

    (yorck/infos yorck/get-yorck-infos-async yorck-infos-chan)

    (go
      (let [yorck-infos (<! yorck-infos-chan)]
        (onto-chan yorck-infos-split-chan yorck-infos)))

    (pipeline-async concurrency imdb-search-chan (partial imdb/search imdb/get-search-page) yorck-infos-split-chan)
    (pipeline-async concurrency imdb-detail-chan (partial imdb/detail imdb/get-detail-page) imdb-search-chan)

    (go-loop [movies []]
      (if-let [movie (<! imdb-detail-chan)]
        (recur (conj movies movie))
        (do
          (>! result-chan (sort-by-rating movies))
          (close! result-chan))))))
