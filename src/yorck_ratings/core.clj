(ns yorck-ratings.core
  (:require [org.httpkit.client :as http]
            [clojure.core.async :as async]
            [hickory.select :as hickory]
            [clojure.string :as string])
  (:use [hickory.core])
  (:import (java.util.regex Pattern)
           (java.net URLEncoder)))

(def imdb-base-url "https://m.imdb.com")
(def yorck-base-url "https://www.yorck.de")

(defrecord RatedMovie [rating rating-count imdb-title imdb-url yorck-title yorck-url])
(defn make-rated-movie
  [{:keys [yorck-title rating rating-count imdb-title imdb-url yorck-url]
    :or   {rating       0
           rating-count 0
           imdb-title   "No title"
           imdb-url     ""
           yorck-url    ""}}]
  (RatedMovie. rating rating-count imdb-title imdb-url yorck-title yorck-url))

(def DEFAULT-TIMEOUT 60000)

(defn- error-message [url cause]
  (str "Error fetching URL \"" url "\": " cause))

(defn get-async [url]
  (let [out (async/chan)]
    (http/get url {:timeout DEFAULT-TIMEOUT}
              (fn [{:keys [status body error]}]
                (async/go
                  (if error
                    (let [{cause :cause} (Throwable->map error)]
                      (error-message url cause))            ; TODO
                    (if (>= status 400)
                      (error-message url status)            ; TODO
                      (async/>! out (as-hickory (parse body)))))
                  (async/close! out))))
    out))

(defn rotate-article [title]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (Der|Die|Das|The)", Pattern/UNICODE_CHARACTER_CLASS)]
    (string/replace-first title pattern "$2 $1")))

(defn remove-dimension [title]
  (let [pattern (Pattern/compile " (- )?2D.*", Pattern/UNICODE_CHARACTER_CLASS)]
    (string/replace-first title pattern "")))

(defn yorck-titles [yorck-page]
  (->> yorck-page
       (hickory/select (hickory/descendant
                         (hickory/class :movie-details)
                         (hickory/tag :h2)))
       (mapcat :content)
       (mapv rotate-article)
       (mapv remove-dimension)))

(defn yorck-urls [yorck-page]
  (->> yorck-page
       (hickory/select (hickory/descendant
                         (hickory/class :movie-details)
                         (hickory/tag :a)))
       (mapv :attrs)
       (map :href)
       (map #(str yorck-base-url %))))

(defn- remove-sneak-preview [movies]
  (remove #(string/includes? (string/lower-case (:yorck-title %)) "sneak") movies))

(defn yorck-titles-urls [yorck-page]
  (map #(make-rated-movie {:yorck-title %1
                           :yorck-url   %2})
       (yorck-titles yorck-page)
       (yorck-urls yorck-page)))

(defn fetch-yorck-infos []
  (async/go
    (let [yorck-page (async/<! (get-async (str yorck-base-url "/filme?filter_today=true")))]
      (->> yorck-page
           (yorck-titles-urls)
           (remove-sneak-preview)))))

(defn imdb-title [search-page]
  (->> search-page
       (hickory/select (hickory/descendant
                         (hickory/class :posters)
                         (hickory/class :poster)
                         (hickory/class :title)
                         (hickory/tag :a)))
       first
       :content
       first))

(defn imdb-url [search-page]
  (->> search-page
       (hickory/select (hickory/descendant
                         (hickory/class :posters)
                         (hickory/class :poster)
                         (hickory/class :title)
                         (hickory/tag :a)))
       (mapv :attrs)
       (mapv :href)
       first
       (str imdb-base-url)))

(defn imdb-rating [detail-page]
  (try
    (->> detail-page
         (hickory/select (hickory/descendant
                           (hickory/id :ratings-bar)
                           hickory/first-child
                           (hickory/class :inline-block)))
         first
         :content
         first
         Double/parseDouble)
    (catch Exception _ 0.0)))

(defn- remove-comma [a-string]
  (string/replace-first a-string "," ""))

(defn imdb-rating-count [detail-page]
  (try
    (->> detail-page
         (hickory/select (hickory/descendant
                           (hickory/id :ratings-bar)
                           hickory/first-child
                           (hickory/class :inline-block)
                           (hickory/class :text-muted)))
         first
         :content
         last
         remove-comma
         Integer/parseInt)
    (catch Exception _ 0)))

(defn- imdb-detail-infos [detail-page]
  {:rating       (imdb-rating detail-page)
   :rating-count (imdb-rating-count detail-page)})

(defn- fetch-imdb-detail [rated-movie result-chan]
  (async/go
    (let [detail-page (async/<! (get-async (:imdb-url rated-movie)))
          updated (merge rated-movie (imdb-detail-infos detail-page))]
      (async/>! result-chan updated)
      (async/close! result-chan))))

(defn imdb-search-infos [search-page]
  {:imdb-title (imdb-title search-page)
   :imdb-url   (imdb-url search-page)})

(defn fetch-imdb-search [rated-movie result-chan]
  (async/go
    (let [title (:yorck-title rated-movie)
          enc-title (URLEncoder/encode title "UTF-8")
          url (str imdb-base-url "/find?q=" enc-title)
          search-page (async/<! (get-async url))
          updated (merge rated-movie (imdb-search-infos search-page))]
      (async/>! result-chan updated)
      (async/close! result-chan))))

(defn- sort-by-rating [movies]
  (reverse (sort-by :rating movies)))

(defn rated-movies [callback]
  (let [yorck-infos-chan (async/chan)
        imdb-search-chan (async/chan)
        imdb-detail-chan (async/chan)]

    (async/go
      (let [yorck-infos (async/<! (fetch-yorck-infos))]
        (doseq [yorck-info yorck-infos]
          (async/>! yorck-infos-chan yorck-info))
        (async/close! yorck-infos-chan)))

    (async/pipeline-async 1 imdb-search-chan fetch-imdb-search yorck-infos-chan)
    (async/pipeline-async 1 imdb-detail-chan fetch-imdb-detail imdb-search-chan)

    (async/go-loop [movies []]
      (if-let [movie (async/<! imdb-detail-chan)]
        (recur (conj movies movie))
        (callback (sort-by-rating movies))))))
