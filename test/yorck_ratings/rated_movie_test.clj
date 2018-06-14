(ns yorck-ratings.rated-movie-test
  (:require [midje.sweet :refer [fact =>]])
  (:require [yorck-ratings.rated-movie :refer [sorted make]]
            [yorck-ratings.fixtures :as fixtures]))

(fact "puts higher rating above lower rating"
      (let [lower-rated (make {:yorck-info  ["title" "#"]
                               :imdb-info   ["title" "#"]
                               :imdb-rating [7.2 10000]})
            higher-rated (make {:yorck-info  ["title" "#"]
                                :imdb-info   ["title" "#"]
                                :imdb-rating [7.3 10000]})
            not-rated (make {:yorck-info ["title" "#"]})]

        (sorted [lower-rated higher-rated]) => [higher-rated lower-rated]
        (sorted [not-rated lower-rated]) => [lower-rated not-rated]))

(fact "puts not rated above rating below threshold"
      (let [below (make {:yorck-info  ["title" "#"]
                         :imdb-info   ["title" "#"]
                         :imdb-rating [6.9 10000]})
            above (make {:yorck-info  ["title" "#"]
                         :imdb-info   ["title" "#"]
                         :imdb-rating [7 10000]})
            not-rated (make {:yorck-info ["title" "#"]})]

        (sorted [above not-rated below]) => [above not-rated below]
        (sorted [above below not-rated]) => [above not-rated below]
        (sorted [not-rated above below]) => [above not-rated below]
        (sorted [not-rated below above]) => [above not-rated below]
        (sorted [below not-rated above]) => [above not-rated below]
        (sorted [below above not-rated]) => [above not-rated below]
        (sorted [below below not-rated]) => [not-rated below below]
        (sorted [below not-rated below]) => [not-rated below below]
        (sorted [not-rated below below]) => [not-rated below below]
        (sorted [above above not-rated]) => [above above not-rated]
        (sorted [above not-rated above]) => [above above not-rated]
        (sorted [not-rated above above]) => [above above not-rated]
        (sorted [not-rated not-rated]) => [not-rated not-rated]))
