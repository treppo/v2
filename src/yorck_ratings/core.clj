(ns yorck-ratings.core
  (:require [org.httpkit.client :as http]
            [clojure.core.async :as a]
            [hickory.select :as h]
            [clojure.string :as str])
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

(defn async-get [url result-ch error-ch]
  (http/get url {:timeout DEFAULT-TIMEOUT}
            (fn [{:keys [status body error]}]
              (a/go
                (if error
                  (let [{cause :cause} (Throwable->map error)]
                    (a/>! error-ch (error-message url cause)))
                  (if (> status 399)
                    (a/>! error-ch (error-message url status))
                    (a/>! result-ch (as-hickory (parse body)))))))))

(defn fetch-yorck-list [result-ch error-ch]
  (async-get (str yorck-base-url "/filme?filter_today=true") result-ch error-ch))

(defn fetch-imdb-sp-info [error-ch {title :yorck-title} result-ch]
  (let [enc-title (URLEncoder/encode title "UTF-8")
        url (str imdb-base-url "/find?q=" enc-title)]
    (async-get url result-ch error-ch)))

(defn rotate-article [title]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (\\w{3})", Pattern/UNICODE_CHARACTER_CLASS)]
    (str/replace-first title pattern "$2 $1")))

(defn yorck-titles [yorck-page]
  (->> yorck-page
       (h/select (h/descendant
                   (h/class :movie-details)
                   (h/tag :h2)))
       (mapcat :content)
       (mapv rotate-article)))

(defn yorck-urls [yorck-page]
  (->> yorck-page
       (h/select (h/descendant
                   (h/class :movie-details)
                   (h/tag :a)))
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
       (h/select (h/descendant
                   (h/class :posters)
                   (h/class :poster)
                   (h/class :title)
                   (h/tag :a)))
       first
       :content
       first))

(defn imdb-url [sp]
  (->> sp
       (h/select (h/descendant
                   (h/class :posters)
                   (h/class :poster)
                   (h/class :title)
                   (h/tag :a)))
       (mapv :attrs)
       (mapv :href)
       first
       (str imdb-base-url)))

(defn imdb-title-url [sp]
  {:imdb-title (imdb-title sp)
   :imdb-url   (imdb-url sp)})

(defn with-imdb-title [movie ch]
  (a/go (merge movie (imdb-title-url (a/<! ch)))))

(defn rated-movies [cb]
  (a/go
    (let [result-ch (a/chan 1 (map yorck-titles-urls))
          error-ch (a/chan)
          imdb-sp-chs (repeatedly (partial a/chan 1))

          _ (fetch-yorck-list result-ch error-ch)

          yorck-infos (a/<! result-ch)

          _ (doall (map (partial fetch-imdb-sp-info error-ch) yorck-infos imdb-sp-chs))
          movie-chs (map with-imdb-title yorck-infos imdb-sp-chs)]

      (a/map (fn [& movies] (cb movies)) movie-chs)
      (a/close! result-ch)
      (a/close! error-ch)
      (map a/close! imdb-sp-chs))))