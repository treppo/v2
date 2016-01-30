(ns yorck-ratings.core
  (:require [org.httpkit.client :as http]
            [clojure.core.async :as a]
            [hickory.select :as h]
            [clojure.string :as str])
  (:use [hickory.core])
  (:import (java.util.regex Pattern)))

(defrecord RatedMovie [rating rating-count imdb-title yorck-title])

(def DEFAULT-TIMEOUT 10000)

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

(defn rotate-article [title]
  (let [pattern (Pattern/compile "^([\\w\\s]+), (\\w{3})", Pattern/UNICODE_CHARACTER_CLASS)]
    (str/replace-first title pattern "$2 $1")))

(def yorck-titles
  (fn [yorck-page]
    (->> yorck-page
         (h/select (h/descendant
                     (h/class :movie-details)
                     (h/tag :h2)))
         (mapcat :content)
         (map rotate-article)
         (map #(RatedMovie. nil nil nil %))
         vec)))

(defn rated-movies
  ([] (rated-movies DEFAULT-TIMEOUT))
  ([timeout]
   (let [result-ch (a/chan 1 (map yorck-titles))
         error-ch (a/chan 1)
         timeout-ch (a/timeout timeout)]
     (async-get "https://www.yorck.de/filme?filter_today=true" result-ch error-ch)
     (a/go
       (let [[result ch] (a/alts! [result-ch timeout-ch])]
         (if result
           result
           (println "Request timed out!")))))))