(ns yorck-ratings.imdb-test
  (:use midje.sweet
        org.httpkit.fake)
  (:require [yorck-ratings.imdb :as imdb]
            [yorck-ratings.fixtures :as fixtures]
            [clojure.core.async :as async]
            [yorck-ratings.rated-movie :as rated-movie]))

(def a-rated-movie (rated-movie/make {}))

(def title "Carol")
(def url fixtures/carol-detail-url)
(def rating fixtures/carol-rating)
(def rating-count fixtures/carol-rating-count)

(defn- get-search-page-stub [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [title url])
    result-chan))

(fact "adds imdb title and url"
      (async/<!! (imdb/search get-search-page-stub a-rated-movie (async/chan))) =>
      (rated-movie/make {:imdb-title title
                         :imdb-url   url}))

(fact "pulls titles and urls from imdb search page"
      (with-fake-http [fixtures/carol-search-url fixtures/carol-search-page]
                      (let [yorck-title fixtures/carol-yorck-title]
                        (async/<!! (imdb/get-search-page yorck-title)) => [title url])))

(defn- get-detail-page-stub [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [rating rating-count])
    result-chan))

(fact "adds imdb rating and rating count"
      (async/<!! (imdb/detail get-detail-page-stub a-rated-movie (async/chan))) =>
      (rated-movie/make {:rating       rating
                         :rating-count rating-count}))

(fact "pulls titles and urls from imdb search page"
      (with-fake-http [fixtures/carol-detail-url fixtures/carol-detail-page]
                      (let [expected [rating rating-count]]
                        (async/<!! (imdb/get-detail-page fixtures/carol-detail-url)) => expected)))
