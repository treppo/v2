(ns yorck-ratings.web-sync
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.reload :as reload]
            [yorck-ratings.core-sync :as core]
            [yorck-ratings.view :as view]
            [config.core :refer [env]]
            [clojure.core.async :as a])
  (:gen-class))

(defn found [body]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    body})

(defn sync-handler [request]
  (found (view/markup (core/rated-movies))))

(defn -main [& args]
  (let [handler (if (:hotreload? env)
                  (reload/wrap-reload #'sync-handler)
                  sync-handler)]
    (run-server handler {:port (Integer/parseInt (:port env))})))
