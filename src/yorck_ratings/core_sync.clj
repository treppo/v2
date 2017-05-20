(ns yorck-ratings.core-sync
  (:require [org.httpkit.client :as http]
            [clojure.core.async :as a]
            [hickory.select :as h]
            [clojure.string :as str]
            [yorck-ratings.yorck-info :as yorck-info])
  (:use [hickory.core])
  (:import (java.util.regex Pattern)
           (java.net URLEncoder)))

(def imdb-base-url "https://m.imdb.com")

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
              (if error
                (let [{cause :cause} (Throwable->map error)]
                  (a/put! error-ch (error-message url cause)))
                (if (> status 399)
                  (a/put! error-ch (error-message url status))
                  (a/put! result-ch (as-hickory (parse body))))))))

(defn fetch-imdb-sp [error-ch {title :yorck-title} result-ch]
  (let [enc-title (URLEncoder/encode title "UTF-8")
        url (str imdb-base-url "/find?q=" enc-title)]
    (async-get url result-ch error-ch)))

(defn fetch-imdb-dp [error-ch movie-ch result-ch]
  (a/go
    (let [movie (a/<! movie-ch)]
      (async-get (:imdb-url movie) result-ch error-ch)
      movie)))

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

(defn imdb-rating [dp]
  (try
    (->> dp
         (h/select (h/descendant
                     (h/id :ratings-bar)
                     h/first-child
                     (h/class :inline-block)))
         first
         :content
         first
         Double/parseDouble)
    (catch Exception e 0.0)))

(defn- remove-comma [s]
  (str/replace-first s "," ""))

(defn imdb-rating-count [dp]
  (try
    (->> dp
         (h/select (h/descendant
                     (h/id :ratings-bar)
                     h/first-child
                     (h/class :inline-block)
                     (h/class :text-muted)))
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
  (a/go (merge movie (imdb-sp-infos (a/<! ch)))))

(defn with-rating [movie-ch dp-ch]
  (a/go
    (let [movie (a/<! movie-ch)
          page (a/<! dp-ch)]
      (merge movie {:rating       (imdb-rating page)
                    :rating-count (imdb-rating-count page)}))))

(defn get-imdb-search-result [title]
  {:imdb-url "" :imdb-title ""})

(defn get-imdb-detail-info [url]
  {:rating 0.0 :rating-count 0})

(defn rated-movies
  ([get-yorck-info-fn get-imdb-search-result-fn get-imdb-detail-info-fn]
   (let [yorck-info (get-yorck-info-fn)
         imdb-search-result (->> yorck-info
                                 (map :yorck-title)
                                 (map get-imdb-search-result-fn))
         imdb-detail-info (->> imdb-search-result
                               (map :imdb-url)
                               (map get-imdb-detail-info-fn))
         merged-infos (map merge yorck-info imdb-search-result imdb-detail-info)
         movies (map make-rated-movie merged-infos)]
     (reverse (sort-by :rating movies))))
  ([] (rated-movies yorck-info/get-info get-imdb-search-result get-imdb-detail-info)))

;(a/go
;  (let [result-ch (a/chan 1 (comp
;                              (map yorck-titles-urls)
;                              (map remove-sneak-preview)))
;        error-ch (a/chan)
;        imdb-sp-chs (repeatedly (partial a/chan 1))
;        imdb-dp-chs (repeatedly (partial a/chan 1))
;
;        _ (fetch-yorck-list result-ch error-ch)
;
;        yorck-infos (a/<! result-ch)
;
;        _ (doall (map (partial fetch-imdb-sp error-ch) yorck-infos imdb-sp-chs))
;        movie-chs (map with-imdb-sp-infos yorck-infos imdb-sp-chs)
;        m-chs (doall (map (partial fetch-imdb-dp error-ch) movie-chs imdb-dp-chs))
;        rated-movie-chs (map with-rating m-chs imdb-dp-chs)]
;
;    (a/map (fn [& movies] (cb (reverse (sort-by :rating movies)))) rated-movie-chs)
;    (a/close! result-ch)
;    (a/close! error-ch)
;    (map a/close! imdb-sp-chs)
;    (map a/close! imdb-dp-chs))))
