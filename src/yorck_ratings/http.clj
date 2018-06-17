(ns yorck-ratings.http
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]
            [clojure.core.async :refer [go chan >! close!]]))

(def DEFAULT-TIMEOUT 10000)

(defn- error-message [url cause]
  (println (str "Error fetching URL \"" url "\": " cause)))

(defn get-async [url]
  (let [out (chan)]
    (client/get url
                {:async?         true
                 :socket-timeout DEFAULT-TIMEOUT
                 :conn-timeout   DEFAULT-TIMEOUT}
                (fn [{:keys [status body]}]
                  (go
                    (if (>= status 400)
                      (error-message url (str "response status code was " status))
                      (>! out (hickory/as-hickory (hickory/parse body))))
                    (close! out)))
                (fn [^Throwable exception]
                  (error-message url (str "exception occurred"))
                  (.printStackTrace exception)))
    out))
