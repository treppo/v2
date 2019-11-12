(ns cinema-ratings.imdb
  (:require [hickory.select :as selector]
            [cinema-ratings.http :as http]
            [clojure.string :as string]
            [cinema-ratings.rated-movie :as rated-movie])
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

(defn get-search-info [cinema-title]
  (let [page (http/get-html (search-url cinema-title))
        title (parse-title page)
        url (parse-url page)]
    (if (and title url)
      [title url]
      [])))

(defn search [get-info-fn rated-movie]
  (if-let [info (not-empty (get-info-fn (rated-movie/cinema-title rated-movie)))]
    (rated-movie/with-imdb-info rated-movie info)
    rated-movie))

(defn- rating [detail-page]
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

(defn- rating-count [detail-page]
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

(defn get-detail-info [url]
  (let [page (http/get-html url)
        rating (rating page)
        count (rating-count page)]
    (if (and rating count)
      [rating count]
      [])))

(defn detail [get-info-fn rated-movie]
  (if (rated-movie/has-imdb-info? rated-movie)
    (if-let [info (not-empty (get-info-fn (rated-movie/imdb-url rated-movie)))]
      (rated-movie/with-imdb-rating rated-movie info)
      rated-movie)
    rated-movie))
