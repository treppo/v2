(ns cinema-ratings.http-test
  (:require [cinema-ratings.http :as http]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer [deftest is]]
            [hickory.core :as hickory]
            [cinema-ratings.fixtures :as fixtures]))

(deftest get-request
  (with-fake-routes-in-isolation
    {fixtures/yorck-list-url (fixtures/yorck-list-ok)}
    (let [parsed-yorck-list-fixture (hickory/as-hickory (hickory/parse fixtures/yorck-list-page))]
      (is (= (http/get-html fixtures/yorck-list-url)
             parsed-yorck-list-fixture)))))
