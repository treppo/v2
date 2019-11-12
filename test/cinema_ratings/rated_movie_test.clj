(ns cinema-ratings.rated-movie-test
  (:require [clojure.test :refer [deftest is]]
            [cinema-ratings.rated-movie :refer [sorted make]]))

(deftest sorting-by-rating
  (let [lower-rated (make {:cinema-info {:title "title"
                                         :url   "#"}
                           :imdb-info   {:title        "title"
                                         :url          "#"
                                         :rating       7.2
                                         :rating-count 10000}})
        higher-rated (make {:cinema-info {:title "title"
                                          :url   "#"}
                            :imdb-info   {:title        "title"
                                          :url          "#"
                                          :rating       7.3
                                          :rating-count 10000}})
        not-rated (make {:cinema-info {:title "title"
                                       :url   "#"}})]

    (is (= (sorted [lower-rated higher-rated]) [higher-rated lower-rated]))
    (is (= (sorted [not-rated lower-rated]) [lower-rated not-rated]))))

(deftest sorting-not-rated
  (let [below (make {:cinema-info {:title "title"
                                   :url   "#"}
                     :imdb-info   {:title        "title"
                                   :url          "#"
                                   :rating       6.9
                                   :rating-count 10000}})
        above (make {:cinema-info {:title "title"
                                   :url   "#"}
                     :imdb-info   {:title        "title"
                                   :url          "#"
                                   :rating       7
                                   :rating-count 10000}})
        not-rated (make {:cinema-info {:title "title"
                                       :url   "#"}})]

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
