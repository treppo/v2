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

(defn- get-yorck-page-async []
  (get-async (str yorck-base-url "/filme?filter_today=true")))

(defn yorck-infos
  ([result-chan] (yorck-infos result-chan get-yorck-page-async))
  ([result-chan get-page-fn]
   (async/go
     (let [yorck-page (async/<! (get-page-fn))]
       (async/>! result-chan (->> yorck-page
                                  (yorck-titles-urls)
                                  (remove-sneak-preview))))
     (async/close! result-chan))))

(defn imdb-title [search-page]
  (->> search-page
       (hickory/select (hickory/descendant
                         (hickory/class :subpage)
                         (hickory/class :h3)))
       first
       :content
       first
       string/trim))

(defn- remove-parameters [path]
  (first (string/split path (Pattern/compile "\\?", Pattern/UNICODE_CHARACTER_CLASS))))

(defn imdb-url [search-page]
  (->> search-page
       (hickory/select (hickory/descendant
                         (hickory/class :subpage)))
       (mapv :attrs)
       (mapv :href)
       first
       remove-parameters
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

(defn- with-imdb-detail-infos [rated-movie detail-page]
  (merge rated-movie {:rating       (imdb-rating detail-page)
                      :rating-count (imdb-rating-count detail-page)}))

(defn imdb-detail
  ([rated-movie result-chan] (imdb-detail rated-movie result-chan get-async))
  ([rated-movie result-chan get-page-fn]
   (async/go
     (let [detail-page (async/<! (get-page-fn (:imdb-url rated-movie)))]
       (async/>! result-chan (with-imdb-detail-infos rated-movie detail-page)))
     (async/close! result-chan))))

(defn with-imdb-search-infos [rated-movie search-page]
  (merge rated-movie {:imdb-title (imdb-title search-page)
                      :imdb-url   (imdb-url search-page)}))

(defn- get-search-page [title]
  (let [enc-title (URLEncoder/encode title "UTF-8")
        url (str imdb-base-url "/find?q=" enc-title)]
    (async/go (async/<! (get-async url)))))

(defn imdb-search
  ([rated-movie result-chan] (imdb-search rated-movie result-chan get-search-page))
  ([rated-movie result-chan get-page-fn]
   (async/go
     (let [search-page (async/<! (get-page-fn (:yorck-title rated-movie)))]
       (async/>! result-chan (with-imdb-search-infos rated-movie search-page)))
     (async/close! result-chan))))

(defn- sort-by-rating [movies]
  (reverse (sort-by :rating movies)))

(defn rated-movies [callback]
  (let [yorck-infos-chan (async/chan)
        yorck-infos-split-chan (async/chan)
        imdb-search-chan (async/chan)
        imdb-detail-chan (async/chan)]

    (yorck-infos yorck-infos-chan)

    (async/go
      (let [yorck-infos (async/<! yorck-infos-chan)]
        (doseq [yorck-info yorck-infos]
          (async/>! yorck-infos-split-chan yorck-info))
        (async/close! yorck-infos-split-chan)))

    (async/pipeline-async 8 imdb-search-chan imdb-search yorck-infos-split-chan)
    (async/pipeline-async 8 imdb-detail-chan imdb-detail imdb-search-chan)

    (async/go-loop [movies []]
      (if-let [movie (async/<! imdb-detail-chan)]
        (recur (conj movies movie))
        (callback (sort-by-rating movies))))))
