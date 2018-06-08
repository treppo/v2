(ns yorck-ratings.web
  (:require [yorck-ratings.core :as core]
            [yorck-ratings.view :as view]
            [org.httpkit.server :refer [run-server with-channel send! close]])
  (:gen-class))

(defn not-found []
  {:status  404
   :headers {"Content-Type" "text/html"}
   :body    "404 Not Found"})

(defn found [body]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    body})

(defn async-handler [req]
  (if (= "/" (:uri req))
    (with-channel req ch
                  (core/rated-movies
                    (fn [movies]
                      (send! ch (found (view/markup movies)))
                      (close ch))))
    (not-found)))

(defn -main [& args]
  (run-server async-handler {:port (Integer/valueOf (or (System/getenv "PORT") "8000"))}))
