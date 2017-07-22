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

(defn async-get [url result-ch]
  (http/get url {:timeout DEFAULT-TIMEOUT}
            (fn [{:keys [status body error]}]
              (if error
                (let [{cause :cause} (Throwable->map error)]
                  (error-message url cause))
                (if (>= status 400)
                  (error-message url status)
                  (async/put! result-ch (as-hickory (parse body))))))))

(defn fetch-yorck-list [result-ch]
  (async-get (str yorck-base-url "/filme?filter_today=true") result-ch))

(defn fetch-yorck-list' [result-ch]
  (async-get (str yorck-base-url "/filme?filter_today=true") result-ch)
  (async/close! result-ch))

(defn fetch-imdb-sp [{title :yorck-title} result-ch]
  (let [enc-title (URLEncoder/encode title "UTF-8")
        url (str imdb-base-url "/find?q=" enc-title)]
    (async-get url result-ch)))

(defn fetch-imdb-dp [movie-ch result-ch]
  (async/go
    (let [movie (async/<! movie-ch)]
      (async-get (:imdb-url movie) result-ch)
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

(defn imdb-title [sp]
  (->> sp
       (hickory/select (hickory/descendant
                         (hickory/class :posters)
                         (hickory/class :poster)
                         (hickory/class :title)
                         (hickory/tag :a)))
       first
       :content
       first))

(defn imdb-url [sp]
  (->> sp
       (hickory/select (hickory/descendant
                         (hickory/class :posters)
                         (hickory/class :poster)
                         (hickory/class :title)
                         (hickory/tag :a)))
       (mapv :attrs)
       (mapv :href)
       first
       (str imdb-base-url)))

(defn imdb-rating [dp]
  (try
    (->> dp
         (hickory/select (hickory/descendant
                           (hickory/id :ratings-bar)
                           hickory/first-child
                           (hickory/class :inline-block)))
         first
         :content
         first
         Double/parseDouble)
    (catch Exception e 0.0)))

(defn- remove-comma [s]
  (string/replace-first s "," ""))

(defn imdb-rating-count [dp]
  (try
    (->> dp
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

(defn imdb-sp-infos [sp]
  {:imdb-title (imdb-title sp)
   :imdb-url   (imdb-url sp)})

(defn with-imdb-sp-infos [movie ch]
  (async/go (merge movie (imdb-sp-infos (async/<! ch)))))

(defn with-rating [movie-ch dp-ch]
  (async/go
    (let [movie (async/<! movie-ch)
          page (async/<! dp-ch)]
      (merge movie {:rating       (imdb-rating page)
                    :rating-count (imdb-rating-count page)}))))

(defn remove-sneak-preview [movies]
  (remove #(string/includes? (string/lower-case (:yorck-title %)) "sneak") movies))

(defn sort-by-rating [movies]
  (reverse (sort-by :rating movies)))

(defn rated-movies [cb]
  (async/go
    (let [result-ch (async/chan 1 (comp
                                    (map yorck-titles-urls)
                                    (map remove-sneak-preview)))
          imdb-sp-chs (repeatedly (partial async/chan 1))
          imdb-dp-chs (repeatedly (partial async/chan 1))

          _ (fetch-yorck-list result-ch)

          yorck-infos (async/<! result-ch)

          _ (doall (map #(fetch-imdb-sp %1 %2) yorck-infos imdb-sp-chs))
          movie-chs (map with-imdb-sp-infos yorck-infos imdb-sp-chs)
          m-chs (doall (map #(fetch-imdb-dp %1 %2) movie-chs imdb-dp-chs))
          rated-movie-chs (map with-rating m-chs imdb-dp-chs)]

      (async/map (fn [& movies] (cb (sort-by-rating movies))) rated-movie-chs)
      (async/close! result-ch)
      (map async/close! imdb-sp-chs)
      (map async/close! imdb-dp-chs))))
