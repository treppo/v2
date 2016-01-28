(ns yorck-ratings.web
  (:use [org.httpkit.server :only [run-server]]
        [hiccup.page])
  (:require [ring.middleware.reload :as reload]
            [yorck-ratings.core :as core]
            [config.core :refer [env]])
  (:gen-class))

(defn handle-not-found []
  {:status  404
   :headers {"Content-Type" "text/html"}
   :body    "404 Not Found"})

(defn markup [movies]
  (html5
    [:head
     [:title "Yorck movies with IMDB ratings"]]
    [:body
     [:ol
      (for [movie movies]
        (let [{:keys [rating imdb-title yorck-title]} movie]
          [:li (str rating " " imdb-title " " yorck-title)]))]]))

(defn show-movies []
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (markup (core/rated-movies))})

(defn app [req]
  (if (= "/" (:uri req))
    (show-movies)
    (handle-not-found)))

(defn -main [& args]
  (let [handler (if (:hotreload? env)
                  (reload/wrap-reload #'app)
                  app)]
    (run-server handler {:port (Integer/parseInt (:port env))})))