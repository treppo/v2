(ns yorck-ratings.http
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [clojure.core.async :refer [go chan >! close!]]))

(def ^:private timeout 10000)

(defn- error-message [url cause]
  (println (str "Error fetching URL \"" url "\": " cause)))

(defn- print-stack-trace [^Throwable exception]
                   (.printStackTrace exception))

(defn get-async [url]
  (let [out (chan)]
    (client/get url
                {:async?         true
                 :socket-timeout timeout
                 :conn-timeout   timeout}
                (fn [{:keys [status body]}]
                  (go
                    (if (>= status 400)
                      (error-message url (str "response status code was " status))
                      (>! out (hickory/as-hickory (hickory/parse body))))
                    (close! out)))
                (fn [exception]
                  (error-message url (str "exception occurred"))
                  (print-stack-trace exception)))
    out))
