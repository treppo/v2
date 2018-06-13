(ns yorck-ratings.imdb
  (:require [hickory.select :as selector]
            [yorck-ratings.http :as http]
            [clojure.string :as string]
            [clojure.core.async :refer [go chan >! <! close! pipeline-async go-loop]]
            [yorck-ratings.rated-movie :as rated-movie])
  (:import (java.net URLEncoder)))

(def base-url "https://m.imdb.com")

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

(defn- with-search-infos [rated-movie [title url]]
  (merge rated-movie {:imdb-title title
                      :imdb-url   url}))

(defn get-search-page [yorck-title]
  (go
    (let [enc-title (URLEncoder/encode yorck-title "UTF-8")
          search-url (str base-url "/find?q=" enc-title)
          page (<! (http/get-async search-url))
          title (parse-title page)
          url (parse-url page)]
      (if (and title url)
        [title url]
        []))))

(defn search [get-infos-fn rated-movie result-chan]
  (go
    (let [search-infos (<! (get-infos-fn (:yorck-title rated-movie)))]
      (if (empty? search-infos)
        (>! result-chan rated-movie)
        (>! result-chan (with-search-infos rated-movie search-infos))))
    (close! result-chan))
  result-chan)

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
    (catch Exception _ 0.0)))

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
    (catch Exception _ 0)))

(defn- with-detail-infos [rated-movie [rating rating-count]]
  (merge rated-movie {:rating       rating
                      :rating-count rating-count}))

(defn get-detail-page [url]
  (go
    (let [page (<! (http/get-async url))]
      [(rating page) (rating-count page)])))

(defn detail [get-page-fn rated-movie result-chan]
  (go
    (if (rated-movie/has-imdb-infos? rated-movie)
      (let [detail-infos (<! (get-page-fn (:imdb-url rated-movie)))]
       (>! result-chan (with-detail-infos rated-movie detail-infos)))
      (>! result-chan rated-movie))
    (close! result-chan))
  result-chan)
