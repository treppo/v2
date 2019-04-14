(ns yorck-ratings.http-test
  (:require [yorck-ratings.http :as http]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer [deftest is]]
            [clojure.core.async :as async]
            [hickory.core :as hickory]
            [yorck-ratings.fixtures :as fixtures]))

(deftest get-request
  (with-fake-routes-in-isolation
    {fixtures/yorck-list-url (fixtures/yorck-list-ok)}
    (let [parsed-yorck-list-fixture (hickory/as-hickory (hickory/parse fixtures/yorck-list-page))]
      (is (= (http/get-html fixtures/yorck-list-url)
             parsed-yorck-list-fixture)))))
