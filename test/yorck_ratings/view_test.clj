(ns yorck-ratings.view-test
  (:require [clojure.test :refer :all]
            [yorck-ratings.view :refer [movie-item]]
            [yorck-ratings.core :refer [make-rated-movie]]))

(def movie1 (make-rated-movie {:yorck-title  "Titanic"
                               :yorck-url    "https://www.yorck.de"
                               :imdb-title   "Titanic"
                               :imdb-url     "https://www.imdb.com"
                               :rating       9.0
                               :rating-count 120000}))

(deftest movie-item-test
  (testing "returns string of movie properties"
    (is (= "9.0 (120000) <a href=\"https://www.imdb.com\">Titanic</a> <a href=\"https://www.yorck.de\">Titanic</a>"
           (movie-item movie1)))))