(ns yorck-ratings.yorck-info
  (:require [hickory.select :as h]
            [org.httpkit.client :as http]
            [clojure.string :as str]
            [hickory.core :as hc])
  (:import (java.util.regex Pattern)))

(def yorck-base-url "https://www.yorck.de")
(def yorck-info-url (str "https://www.yorck.de/filme?filter_today=true"))

(defn get-page [url]
  (let [{:keys [body error]} @(http/get url)
        parsed-response (hc/as-hickory (hc/parse body))]
    (if error
      (println "Failed, exception: " error)
      parsed-response)))

(defn rotate-article [title]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (Der|Die|Das|The)", Pattern/UNICODE_CHARACTER_CLASS)]
    (str/replace-first title pattern "$2 $1")))

(defn remove-dimension [title]
  (let [pattern (Pattern/compile " (- )?2D.*", Pattern/UNICODE_CHARACTER_CLASS)]
    (str/replace-first title pattern "")))

(defn sneak-preview? [movie]
  (str/includes? (str/lower-case (:yorck-title movie)) "sneak"))

(defn titles [yorck-page]
  (->> yorck-page
       (h/select (h/descendant
                   (h/class :movie-details)
                   (h/tag :h2)))
       (mapcat :content)
       (mapv rotate-article)
       (mapv remove-dimension)))

(defn urls [yorck-page]
  (->> yorck-page
       (h/select (h/descendant
                   (h/class :movie-details)
                   (h/tag :a)))
       (mapv :attrs)
       (map :href)
       (map #(str yorck-base-url %))))

(defn make-yorck-info [title url] {:yorck-title title :yorck-url url})

(defn get-info
  ([]
   (get-info get-page))

  ([get-page-fn]
   (let [yorck-page (get-page-fn yorck-info-url)
         titles (titles yorck-page)
         urls (urls yorck-page)]
     (->> (map make-yorck-info titles urls)
          (remove sneak-preview?)))))
