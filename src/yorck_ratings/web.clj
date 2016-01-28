(ns yorck-ratings.web
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.reload :as reload]
            [yorck-ratings.core :as core]
            [yorck-ratings.view :as view]
            [config.core :refer [env]])
  (:gen-class))

(defn handle-not-found []
  {:status  404
   :headers {"Content-Type" "text/html"}
   :body    "404 Not Found"})

(defn show-movies []
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (view/markup (core/rated-movies))})

(defn app [req]
  (if (= "/" (:uri req))
    (show-movies)
    (handle-not-found)))

(defn -main [& args]
  (let [handler (if (:hotreload? env)
                  (reload/wrap-reload #'app)
                  app)]
    (run-server handler {:port (Integer/parseInt (:port env))})))