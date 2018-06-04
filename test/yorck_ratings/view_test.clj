(ns yorck-ratings.view-test
  (:use midje.sweet)
  (:require [yorck-ratings.view :refer [movie-item]]
            [yorck-ratings.fixtures :as fixtures]))

(fact "returns string of movie properties"
      (movie-item fixtures/carol-rated-movie) => "7.2 (89891) • <a href=\"https://m.imdb.com/title/tt2402927/\">Carol</a> • <a href=\"https://www.yorck.de/filme/carol\">Carol</a>")
