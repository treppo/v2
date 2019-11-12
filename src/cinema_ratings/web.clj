(ns cinema-ratings.web
  (:require [cinema-ratings.modules :as modules]
            [cinema-ratings.view :as view]
            [clojure.core.async :refer [go <! chan close!]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn- not-found []
  {:status  404
   :headers {"Content-Type" "text/plain"}
   :body    "404 Not Found"})

(defn- found [body]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})

(defn async-handler [req success-fn error-fn]
  (if (= "/" (:uri req))
    (go
      (let [result-chan (chan)]
        (modules/rated-movies result-chan)
        (success-fn (found (view/markup (<! result-chan))))))
    (not-found)))

(defn- ^String port []
  (or (System/getenv "PORT") "8000"))

(defn -main [& args]
  (run-jetty async-handler
             {:async? true
              :port  (Integer/valueOf (port))}))
