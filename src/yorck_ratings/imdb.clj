(ns yorck-ratings.imdb
  (:require [hickory.select :as selector]
            [yorck-ratings.http :as http]
            [clojure.string :as string]
            [clojure.core.async :refer [go chan >! <! close! pipeline-async go-loop]])
  (:import (java.net URLEncoder)
           (java.util.regex Pattern)))

(def base-url "https://m.imdb.com")

(defn- parse-title [search-page]
  (->> search-page
       (selector/select (selector/descendant
                          (selector/class :subpage)
                          (selector/class :h3)))
       first
       :content
       first
       string/trim))

(defn- remove-parameters [path]
  (first (string/split path (Pattern/compile "\\?", Pattern/UNICODE_CHARACTER_CLASS))))

(defn- parse-url [search-page]
  (->> search-page
       (selector/select (selector/descendant
                          (selector/class :subpage)))
       (mapv :attrs)
       (mapv :href)
       first
       remove-parameters
       (str base-url)))

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
    (let [detail-infos (<! (get-page-fn (:imdb-url rated-movie)))]
      (>! result-chan (with-detail-infos rated-movie detail-infos)))
    (close! result-chan))
  result-chan)

(defn- with-search-infos [rated-movie [title url]]
  (merge rated-movie {:imdb-title title
                      :imdb-url   url}))

(defn get-search-page [yorck-title]
  (go
    (let [enc-title (URLEncoder/encode yorck-title "UTF-8")
          search-url (str base-url "/find?q=" enc-title)
          page (<! (http/get-async search-url))]
      [(parse-title page) (parse-url page)])))

(defn search [get-page-fn rated-movie result-chan]
  (go
    (let [search-infos (<! (get-page-fn (:yorck-title rated-movie)))]
      (>! result-chan (with-search-infos rated-movie search-infos)))
    (close! result-chan))
  result-chan)
