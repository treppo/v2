(ns yorck-ratings.http-test
  (:require [yorck-ratings.http :as http]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact =>]]
            [clojure.core.async :as async]
            [hickory.core :as hickory]
            [yorck-ratings.fixtures :as fixtures]))

(fact "writes parsed successful get request result to channel"
      (with-fake-routes-in-isolation
        {fixtures/yorck-list-url (fixtures/yorck-list-ok)}
        (let [result-ch (http/get-async fixtures/yorck-list-url)
              parsed-yorck-list-fixture (hickory/as-hickory (hickory/parse fixtures/yorck-list-page))]

          (async/<!! result-ch) => parsed-yorck-list-fixture)))
