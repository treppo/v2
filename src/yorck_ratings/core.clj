(ns yorck-ratings.core
  (:require [org.httpkit.client :as http]
            [clojure.core.async :as a]
            [hickory.select :as h])
  (:use [hickory.core]))

(defrecord RatedMovie [rating rating-count imdb-title yorck-title])

(defn- error-message [url cause]
  (str "Error fetching URL \"" url "\": " cause))

(defn async-get [url result-ch error-ch]
  (http/get url
            (fn [{:keys [status body error]}]
              (a/go
                (if error
                  (let [{cause :cause} (Throwable->map error)]
                    (a/>! error-ch (error-message url cause)))
                  (if (> status 399)
                    (a/>! error-ch (error-message url status))
                    (a/>! result-ch (as-hickory (parse body)))))))))

(def yorck-titles
  (fn [yorck-page]
    (->> yorck-page
         (h/select (h/descendant
                     (h/class :movie-details)
                     (h/tag :h2)))
         (mapcat :content)
         (map #(RatedMovie. nil nil nil %))
         vec)))

(defn rated-movies
  ([] (rated-movies 10000))
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