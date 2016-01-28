(ns yorck-ratings.view-test
  (:require [clojure.test :refer :all]
            [yorck-ratings.view :refer [movie-item]])
  (:import (yorck_ratings.core RatedMovie)))

(def movie1 (RatedMovie. "9.0" "Titanic" "Titanic"))

(deftest movie-item-test
  (testing "Returns string of movie properties separated by spaces"
    (is (= "9.0 Titanic Titanic" (movie-item movie1)))))