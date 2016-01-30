(ns yorck-ratings.view-test
  (:require [clojure.test :refer :all]
            [yorck-ratings.view :refer [movie-item]])
  (:import (yorck_ratings.core RatedMovie)))

(def movie1 (RatedMovie. 9.0 120000 "Titanic" "Titanic"))

(deftest movie-item-test
  (testing "returns string of movie properties"
    (is (= "9.0 (120000) Titanic Titanic" (movie-item movie1)))))