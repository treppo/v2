(ns yorck-ratings.http
  (:require [clj-http.client :as client]
            [hickory.core :as hickory]))

(def ^:private timeout 10000)

(def ^:private options {:socket-timeout timeout :conn-timeout timeout})

(defn- error-message [url cause]
  (println (str "Error fetching URL \"" url "\": " cause)))

(defn get-html [url]
  (let [{:keys [status body]} (client/get url options)]
    (if (< status 400)
      (hickory/as-hickory (hickory/parse body))
      (error-message url (str "response status code was " status)))))
