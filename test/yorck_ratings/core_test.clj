(ns yorck-ratings.core-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [yorck-ratings.core :refer :all]
            [clojure.java.io :as io])
  (:import (yorck_ratings.core RatedMovie)))

(defn load-fixture [filename]
  (->> filename
      (str "fixtures/")
      (io/resource)
      (slurp)))

(def detail-page-fixture (load-fixture "hateful_8_detail_page.html"))

(def search-page-fixture (load-fixture "hateful_8_search_page.html"))

(def yorck-list-fixture (load-fixture "yorck_list.html"))

(deftest end-to-end-test
  (testing "show list of movies with rating"
    (with-fake-http ["https://www.yorck.de/filme?filter_today=true" yorck-list-fixture
                     "http://www.imdb.com/find?q=The+Hateful+8" search-page-fixture
                     "http://m.imdb.com/title/tt3460252/" detail-page-fixture]
                    (is (= [(RatedMovie. 8 "The Hateful Eight (2015)" "The Hateful Eight")] (rated-movies))))))
