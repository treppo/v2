(ns yorck-ratings.imdb
  (:require [hickory.select :as selector]
            [yorck-ratings.http :as http]
            [clojure.string :as string]
            [clojure.core.async :refer [go chan >! <! close!]]
            [yorck-ratings.rated-movie :as rated-movie])
  (:import (java.net URLEncoder)))

(def ^:private base-url "https://m.imdb.com")

(defn- parse-title [search-page]
  (try
    (->> search-page
         (selector/select (selector/descendant
                            (selector/class :subpage)
                            (selector/class :h3)))
         first
         :content
         first
         string/trim)
    (catch Exception _ nil)))

(defn- remove-parameters [path]
  (first (string/split path #"\?")))

(defn- parse-url [search-page]
  (try
    (->> search-page
         (selector/select (selector/descendant
                            (selector/class :subpage)))
         (mapv :attrs)
         (mapv :href)
         first
         remove-parameters
         (str base-url))
    (catch Exception _ nil)))

(defn- url-encode [^String s]
  (URLEncoder/encode s "UTF-8"))

(defn- search-url [title]
  (str base-url "/find?q=" (url-encode title)))

(defn get-search-page [yorck-title]
  (go
    (let [page (<! (http/get-async (search-url yorck-title)))
          title (parse-title page)
          url (parse-url page)]
      (if (and title url)
        [title url]
        []))))

(defn- pipeline-skip-when-empty [to merge-fn rated-movie from]
  (go
    (if-let [info (not-empty (<! from))]
      (>! to (merge-fn rated-movie info))
      (>! to rated-movie))
    (close! to)))

(defn search [get-infos-fn rated-movie result-chan]
  (let [info-channel (get-infos-fn (rated-movie/yorck-title rated-movie))]
    (pipeline-skip-when-empty result-chan rated-movie/with-imdb-info rated-movie info-channel)))

(defn rating [detail-page]
  (try
    (->> detail-page
         (selector/select (selector/descendant
                            (selector/id :ratings-bar)
                            selector/first-child
                            (selector/class :inline-block)))
         first
         :content
         first
         Double/parseDouble)
    (catch Exception e nil)))

(defn- remove-comma [a-string]
  (string/replace-first a-string "," ""))

(defn rating-count [detail-page]
  (try
    (->> detail-page
         (selector/select (selector/descendant
                            (selector/id :ratings-bar)
                            selector/first-child
                            (selector/class :inline-block)
                            (selector/class :text-muted)))
         first
         :content
         last
         remove-comma
         Integer/parseInt)
    (catch Exception e nil)))

(defn get-detail-page [url]
  (go
    (let [page (<! (http/get-async url))
          rating (rating page)
          count (rating-count page)]
      (if (and rating count)
        [rating count]
        []))))

(def continue? rated-movie/has-imdb-info?)

(defn detail [get-page-fn rated-movie result-chan]
  (let [info-channel (get-page-fn (rated-movie/imdb-url rated-movie))]
    (pipeline-skip-when-empty result-chan rated-movie/with-imdb-rating rated-movie info-channel)))
