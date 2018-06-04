(ns yorck-ratings.http-test
  (:use org.httpkit.fake
        midje.sweet)
  (:require [yorck-ratings.http :as http]
            [clojure.core.async :as async]
            [hickory.core :as hickory]
            [yorck-ratings.fixtures :as fixtures]))

(def parsed-yorck-list-fixture (hickory/as-hickory (hickory/parse fixtures/yorck-list-page)))

(fact "writes parsed successful get request result to channel"
      (with-fake-http [fixtures/yorck-list-url fixtures/yorck-list-page]
                      (let [result-ch (http/get-async fixtures/yorck-list-url)]
                        (async/<!! result-ch) => parsed-yorck-list-fixture)))
