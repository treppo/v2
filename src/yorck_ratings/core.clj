(ns yorck-ratings.core
  (:require [yorck-ratings.yorck :as yorck]
            [yorck-ratings.imdb :as imdb]
            [yorck-ratings.rated-movie :as rated-movie]
            [clojure.core.async :refer [go chan >! <! close! pipeline-async go-loop onto-chan]]))

(defn- split [to from]
  (go
    (let [items (<! from)]
      (onto-chan to items))))

(defn- join [to f from]
  (go-loop [collection []]
    (if-let [x (<! from)]
      (recur (conj collection x))
      (do
        (>! to (f collection))
        (close! to)))))

(defn skip [condition f]
  (fn [item out]
    (go
      (if (condition item)
        (f item out)
        (do
          (>! out item)
          (close! out))))))

(defn rated-movies [result-chan]
  (let [yorck-infos-chan (chan)
        yorck-infos-split-chan (chan)
        imdb-search-chan (chan)
        imdb-detail-chan (chan)
        concurrency (.availableProcessors (Runtime/getRuntime))]

    (yorck/infos yorck/get-yorck-infos-async yorck-infos-chan)

    (split yorck-infos-split-chan yorck-infos-chan)
    (pipeline-async concurrency imdb-search-chan (partial imdb/search imdb/get-search-page) yorck-infos-split-chan)
    (pipeline-async concurrency imdb-detail-chan (skip imdb/continue? (partial imdb/detail imdb/get-detail-page)) imdb-search-chan)
    (join result-chan rated-movie/sorted imdb-detail-chan)))
