(ns yorck-ratings.web
  (:use [org.httpkit.server :only [run-server with-channel send! close]])
  (:require [ring.middleware.reload :as reload]
            [yorck-ratings.core :as core]
            [yorck-ratings.view :as view]
            [config.core :refer [env]]
            [clojure.core.async :as a])
  (:gen-class))

(defn not-found []
  {:status  404
   :headers {"Content-Type" "text/html"}
   :body    "404 Not Found"})

(defn show-movies []
  (a/go {:status  200
         :headers {"Content-Type" "text/html"}
         :body    (view/markup (a/<! (core/rated-movies)))}))

(defn async-handler [req]
  (if (= "/" (:uri req))
    (with-channel req ch
                  (a/go
                    (send! ch (a/<! (show-movies)))
                    (close ch)))
    (not-found)))

(defn -main [& args]
  (let [handler (if (:hotreload? env)
                  (reload/wrap-reload #'async-handler)
                  async-handler)]
    (run-server handler {:port (Integer/parseInt (:port env))})))