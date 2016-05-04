(ns yorck-ratings.view-test
  (:require [clojure.test :refer :all]
            [yorck-ratings.view :refer [movie-item]]
            [yorck-ratings.core :refer [make-rated-movie]]))

(def movie1 (make-rated-movie {:yorck-title  "Titanic"
                               :imdb-title   "Titanic"
                               :rating       9.0
                               :rating-count 120000}))

(deftest movie-item-test
  (testing "returns string of movie properties"
    (is (= "9.0 (120000) Titanic Titanic" (movie-item movie1)))))