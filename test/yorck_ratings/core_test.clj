(ns yorck-ratings.core-test
  (:use org.httpkit.fake
        hickory.core)
  (:require [clojure.test :refer :all]
            [yorck-ratings.core :refer :all]
            [clojure.java.io :as io]
            [clojure.core.async :as a])
  (:import (yorck_ratings.core RatedMovie)))

(defn load-fixture [filename]
  (->> filename
       (str "fixtures/")
       (io/resource)
       (slurp)))

(def yorck-list-fixture (load-fixture "yorck_list.html"))
(def parsed-yorck-list-fixture (as-hickory (parse yorck-list-fixture)))
(def hateful-8-dp-fixture (load-fixture "hateful_8_detail_page.html"))
(def hateful-8-sp-fixture (load-fixture "hateful_8_search_page.html"))
(def carol-dp-fixture (load-fixture "carol_detail_page.html"))
(def carol-sp-fixture (load-fixture "carol_search_page.html"))
(def yorck-list-url "https://www.yorck.de/filme?filter_today=true")

(deftest end-to-end-test
  (testing "show yorck titles"
    (with-fake-http [yorck-list-url yorck-list-fixture
                     "https://www.imdb.com/find?q=The+Hateful+8" hateful-8-sp-fixture
                     "https://m.imdb.com/title/tt3460252/" hateful-8-dp-fixture
                     "https://www.imdb.com/find?q=Carol" carol-sp-fixture
                     "https://m.imdb.com/title/tt2402927/" carol-dp-fixture]
                    (let [expected [(RatedMovie. nil nil nil "Hateful 8, The")
                                    (RatedMovie. nil nil nil "Carol")]]
                      (is (= expected (a/<!! (rated-movies 100))))))))

(deftest async-get-test
  (testing "writes parsed successful get request result to channel"
    (with-fake-http [yorck-list-url yorck-list-fixture]
                    (let [result-ch (a/chan 1)
                          error-ch (a/chan 1)]
                      (async-get yorck-list-url result-ch error-ch)
                      (is (= parsed-yorck-list-fixture (a/<!! result-ch))))))

  (testing "writes request error to error channel"
    (let [result-ch (a/chan 1)
          error-ch (a/chan 1)
          expected "Error fetching URL \"http://non-existant-url.kentucky\": non-existant-url.kentucky: unknown error"]
      (async-get "http://non-existant-url.kentucky" result-ch error-ch)
      (is (= expected (a/<!! error-ch)))))

  (testing "writes successful request with exceptional response to error channel"
    (with-fake-http ["http://m.imdb.com/non-existant-uri" 404]
                    (let [result-ch (a/chan 1)
                          error-ch (a/chan 1)
                          expected "Error fetching URL \"http://m.imdb.com/non-existant-uri\": 404"]
                      (async-get "http://m.imdb.com/non-existant-uri" result-ch error-ch)
                      (is (= expected (a/<!! error-ch)))))))

(deftest yorck-titles-test
  (testing "returns yorck movie titles"
    (let [expected [(RatedMovie. nil nil nil "Hateful 8, The")
                    (RatedMovie. nil nil nil "Carol")]]
      (is (= expected (yorck-titles parsed-yorck-list-fixture))))))