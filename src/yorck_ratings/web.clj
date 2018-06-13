(ns yorck-ratings.web
  (:require [yorck-ratings.core :as core]
            [yorck-ratings.view :as view]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn not-found []
  {:status  404
   :headers {"Content-Type" "text/plain"}
   :body    "404 Not Found"})

(defn found [body]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})

(defn async-handler [req success-fn error-fn]
  (if (= "/" (:uri req))
    (core/rated-movies
      (fn [movies]
        (success-fn (found (view/markup movies)))))
    (not-found)))

(defn -main [& args]
  (run-jetty async-handler
             {:async? true
              :port  (Integer/valueOf (or (System/getenv "PORT") "8000"))}))
