(ns yorck-ratings.yorck
  (:require [hickory.select :as selector]
            [clojure.string :as string]
            [yorck-ratings.http :as http]
            [yorck-ratings.rated-movie :as rated-movie]
            [clojure.core.async :refer [go chan >! <! close! pipeline-async go-loop]])
  (:import (java.util.regex Pattern)))

(def ^:private yorck-base-url "https://www.yorck.de")

(defn- yorck-titles [yorck-page]
  (->> yorck-page
       (selector/select (selector/descendant
                          (selector/class :movie-details)
                          (selector/tag :h2)))
       (mapcat :content)))

(defn- yorck-urls [yorck-page]
  (->> yorck-page
       (selector/select (selector/descendant
                          (selector/class :movie-details)
                          (selector/tag :a)))
       (mapv :attrs)
       (map :href)
       (map #(str yorck-base-url %))))

(defn- yorck-titles-urls [yorck-page]
  (map vector (yorck-titles yorck-page) (yorck-urls yorck-page)))

(defn get-yorck-infos-async []
  (go (yorck-titles-urls (<! (http/get-async (str yorck-base-url "/filme?filter_today=true"))))))

(defn- is-sneak-preview [[title _]]
  (string/includes? (string/lower-case title) "sneak"))

(defn- rotate-article [[title url]]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (Der|Die|Das|The)", Pattern/UNICODE_CHARACTER_CLASS)]
    [(string/replace-first title pattern "$2 $1") url]))

(defn- remove-dimension [[title url]]
  [(string/replace-first title #" (- )?2D.*" "") url])

(defn infos [get-page-fn result-chan]
  (go
    (let [yorck-infos (<! (get-page-fn))]
      (>! result-chan (->> yorck-infos
                           (remove is-sneak-preview)
                           (mapv remove-dimension)
                           (mapv rotate-article)
                           (mapv rated-movie/from-yorck-info))))
    (close! result-chan)))
