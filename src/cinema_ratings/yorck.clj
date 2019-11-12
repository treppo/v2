(ns cinema-ratings.yorck
  (:require [hickory.select :as selector]
            [clojure.string :as string]
            [cinema-ratings.http :as http]
            [cinema-ratings.rated-movie :as rated-movie])
  (:import (java.util.regex Pattern)))

(def ^:private yorck-base-url "https://www.yorck.de")

(defn- yorck-titles [yorck-page]
  (->> yorck-page
       (selector/select (selector/descendant
                          (selector/class :movie-details)
                          (selector/tag :h2)))
       (mapcat :content)))

(defn- yorck-urls [yorck-page]
  (->> yorck-page
       (selector/select (selector/descendant
                          (selector/class :movie-details)
                          (selector/tag :a)))
       (mapv :attrs)
       (map :href)
       (map #(str yorck-base-url %))))

(defn- yorck-titles-urls [yorck-page]
  (map vector (yorck-titles yorck-page) (yorck-urls yorck-page)))

(defn get-cinema-info []
  (yorck-titles-urls (http/get-html (str yorck-base-url "/filme?filter_today=true"))))

(defn- is-sneak-preview [[title _]]
  (string/includes? (string/lower-case title) "sneak"))

(defn- rotate-article [[title url]]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (Der|Die|Das|The)", Pattern/UNICODE_CHARACTER_CLASS)]
    [(string/replace-first title pattern "$2 $1") url]))

(defn- remove-dimension [[title url]]
  [(string/replace-first title #" (- )?2D.*" "") url])

(defn- remove-premiere [[title url]]
  [(string/replace-first title #" - Premiere" "") url])

(defn info [get-page-fn]
  (->> (get-page-fn)
       (remove is-sneak-preview)
       (mapv remove-dimension)
       (mapv remove-premiere)
       (mapv rotate-article)
       (mapv rated-movie/from-cinema-info)))
