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

(defn async-get [url result-chan]
  (http/get url {:timeout DEFAULT-TIMEOUT}
            (fn [{:keys [status body error]}]
              (if error
                (let [{cause :cause} (Throwable->map error)]
                  (error-message url cause))
                (if (>= status 400)
                  (error-message url status)
                  (async/go (async/>! result-chan (as-hickory (parse body)))))))))

(defn fetch-yorck-list [result-chan]
  (async-get (str yorck-base-url "/filme?filter_today=true") result-chan))

(defn fetch-imdb-sp [{title :yorck-title} result-chan]
  (let [enc-title (URLEncoder/encode title "UTF-8")
        url (str imdb-base-url "/find?q=" enc-title)]
    (async-get url result-chan)))

(defn fetch-imdb-detail [movie-chan result-chan]
  (async/go
    (let [movie (async/<! movie-chan)]
      (async-get (:imdb-url movie) result-chan)
      movie)))

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

(defn yorck-titles-urls [yorck-page]
  (map #(make-rated-movie {:yorck-title %1
                           :yorck-url   %2})
       (yorck-titles yorck-page)
       (yorck-urls yorck-page)))

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
    (catch Exception e 0.0)))

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
    (catch Exception e 0)))

(defn imdb-search-infos [search-page]
  {:imdb-title (imdb-title search-page)
   :imdb-url   (imdb-url search-page)})

(defn with-imdb-search-infos [movie chan]
  (async/go (merge movie (imdb-search-infos (async/<! chan)))))

(defn with-rating [movie-chan imdb-detail-chan]
  (async/go
    (let [movie (async/<! movie-chan)
          detail-page (async/<! imdb-detail-chan)]
      (merge movie {:rating       (imdb-rating detail-page)
                    :rating-count (imdb-rating-count detail-page)}))))

(defn remove-sneak-preview [movies]
  (remove #(string/includes? (string/lower-case (:yorck-title %)) "sneak") movies))

(defn sort-by-rating [movies]
  (reverse (sort-by :rating movies)))

(defn rated-movies [callback]
  (async/go
    (let [yorck-chan (async/chan 1 (comp
                                    (map yorck-titles-urls)
                                    (map remove-sneak-preview)))
          imdb-search-chs (repeatedly (partial async/chan 1))
          imdb-detail-chs (repeatedly (partial async/chan 1))

          _ (fetch-yorck-list yorck-chan)

          yorck-infos (async/<! yorck-chan)

          _ (doall (map #(fetch-imdb-sp %1 %2) yorck-infos imdb-search-chs))
          movie-chs (map with-imdb-search-infos yorck-infos imdb-search-chs)
          m-chs (doall (map #(fetch-imdb-detail %1 %2) movie-chs imdb-detail-chs))
          rated-movie-chs (map with-rating m-chs imdb-detail-chs)]

      (async/map (fn [& movies] (callback (sort-by-rating movies))) rated-movie-chs)
      (async/close! yorck-chan)
      (map async/close! imdb-search-chs)
      (map async/close! imdb-detail-chs))))
