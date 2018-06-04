(ns yorck-ratings.http
  (:require [org.httpkit.client :as http]
            [hickory.core :as hickory]
            [clojure.core.async :refer [go chan >! close!]]))

(def DEFAULT-TIMEOUT 60000)

(defn- error-message [url cause]
  (str "Error fetching URL \"" url "\": " cause))

(defn get-async [url]
  (let [out (chan)]
    (http/get url {:timeout DEFAULT-TIMEOUT}
              (fn [{:keys [status body error]}]
                (go
                  (if error
                    (let [{cause :cause} (Throwable->map error)]
                      (error-message url cause))            ; TODO
                    (if (>= status 400)
                      (error-message url status)            ; TODO
                      (>! out (hickory/as-hickory (hickory/parse body)))))
                  (close! out))))
    out))
