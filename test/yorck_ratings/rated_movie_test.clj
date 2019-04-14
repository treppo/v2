(ns yorck-ratings.rated-movie-test
  (:require [clojure.test :refer [deftest is]]
            [yorck-ratings.rated-movie :refer [sorted make]]
            [yorck-ratings.fixtures :as fixtures]))

(deftest sorting-by-rating
  (let [lower-rated (make {:yorck-info  ["title" "#"]
                           :imdb-info   ["title" "#"]
                           :imdb-rating [7.2 10000]})
        higher-rated (make {:yorck-info  ["title" "#"]
                            :imdb-info   ["title" "#"]
                            :imdb-rating [7.3 10000]})
        not-rated (make {:yorck-info ["title" "#"]})]

    (is (= (sorted [lower-rated higher-rated]) [higher-rated lower-rated]))
    (is (= (sorted [not-rated lower-rated]) [lower-rated not-rated]))))

(deftest sorting-not-rated
  (let [below (make {:yorck-info  ["title" "#"]
                     :imdb-info   ["title" "#"]
                     :imdb-rating [6.9 10000]})
        above (make {:yorck-info  ["title" "#"]
                     :imdb-info   ["title" "#"]
                     :imdb-rating [7 10000]})
        not-rated (make {:yorck-info ["title" "#"]})]

    (is (= (sorted [above not-rated below]) [above not-rated below]))
    (is (= (sorted [above below not-rated]) [above not-rated below]))
    (is (= (sorted [not-rated above below]) [above not-rated below]))
    (is (= (sorted [not-rated below above]) [above not-rated below]))
    (is (= (sorted [below not-rated above]) [above not-rated below]))
    (is (= (sorted [below above not-rated]) [above not-rated below]))
    (is (= (sorted [below below not-rated]) [not-rated below below]))
    (is (= (sorted [below not-rated below]) [not-rated below below]))
    (is (= (sorted [not-rated below below]) [not-rated below below]))
    (is (= (sorted [above above not-rated]) [above above not-rated]))
    (is (= (sorted [above not-rated above]) [above above not-rated]))
    (is (= (sorted [not-rated above above]) [above above not-rated]))
    (is (= (sorted [not-rated not-rated]) [not-rated not-rated]))))
