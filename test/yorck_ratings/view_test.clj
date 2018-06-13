(ns yorck-ratings.view-test
  (:use midje.sweet)
  (:require [yorck-ratings.view :refer [movie-item]]
            [yorck-ratings.fixtures :as fixtures]))

(fact "returns string of movie properties"
      (movie-item fixtures/carol-rated-movie) =>
      [:li {:class "rated-movie highlighted"}
       "7.2 (89891) • "
       [:a {:href fixtures/carol-detail-url} "Carol"]
       " • "
       [:a {:href fixtures/carol-yorck-url} fixtures/carol-yorck-title]])
