(ns cinema-ratings.cinema.yorck
  (:require [hickory.select :as selector]
            [clojure.string :as string]
            [cinema-ratings.http :as http]
            [cinema-ratings.rated-movie :as rated-movie]
            [clojure.spec.alpha :as spec])
  (:import (java.util.regex Pattern)))

(def ^:private yorck-base-url "https://www.yorck.de")
(def ^:private yorck-today-url (str yorck-base-url "/filme?filter_today=true"))

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
  (map
   (fn [title url] {:title title :url url})
   (yorck-titles yorck-page)
   (yorck-urls yorck-page)))

(defn get-cinema-info []
  (yorck-titles-urls (http/get-html yorck-today-url)))

(defn- is-sneak-preview [{:keys [title]}]
  (string/includes? (string/lower-case title) "sneak"))

(defn- rotate-article [{:keys [title url]}]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (Der|Die|Das|The)", Pattern/UNICODE_CHARACTER_CLASS)]
    {:title (string/replace-first title pattern "$2 $1") :url url}))

(defn- remove-dimension [{:keys [title url]}]
  {:title (string/replace-first title #" (- )?2D.*" "") :url url})

(defn- remove-premiere [{:keys [title url]}]
  {:title (string/replace-first title #" - Premiere" "") :url url})

(defn info [get-info-fn]
  (->> (get-info-fn)
       (remove is-sneak-preview)
       (mapv remove-dimension)
       (mapv remove-premiere)
       (mapv rotate-article)
       (mapv rated-movie/from-cinema-info)))

(spec/def ::get-info-fn
  (spec/fspec
   :args empty?
   :ret (spec/coll-of ::rated-movie/cinema-info)))

(spec/def get-cinema-info ::get-info-fn)

(spec/fdef info
  :args (spec/cat :function ::get-info-fn)
  :ret ::rated-movie/rated-movie)
