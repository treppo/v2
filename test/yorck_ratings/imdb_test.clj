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

(defn- get-search-infos-stub [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [title url])
    result-chan))

(defn- make-get-search-infos-stub [infos]
  (fn [_]
    (let [result-chan (async/chan)]
      (async/put! result-chan infos)
      result-chan)))

(fact "adds imdb title and url"
      (async/<!! (imdb/search (make-get-search-infos-stub [title url]) a-rated-movie (async/chan))) =>
      (rated-movie/make {:imdb-title title
                         :imdb-url   url}))

(fact "does not add anything if the movie can't be found on imdb"
      (async/<!! (imdb/search (make-get-search-infos-stub []) a-rated-movie (async/chan))) =>
      (rated-movie/make {}))

(fact "pulls titles and urls from imdb search page"
      (with-fake-http [fixtures/carol-search-url fixtures/carol-search-page]
                      (let [yorck-title fixtures/carol-yorck-title]
                        (async/<!! (imdb/get-search-page yorck-title)) => [title url])))

(fact "returns no detail infos if the movie can not be found on imdb"
      (with-fake-http [fixtures/no-search-result-search-url fixtures/no-search-result-search-page]
                      (async/<!! (imdb/get-search-page fixtures/no-search-result-yorck-title)) => []))

(defn- get-detail-page-stub [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [rating rating-count])
    result-chan))

(fact "adds imdb rating and rating count"
      (async/<!! (imdb/detail get-detail-page-stub a-rated-movie (async/chan))) =>
      (rated-movie/make {:rating       rating
                         :rating-count rating-count}))

(fact "pulls rating and rating count from imdb detail page"
      (with-fake-http [fixtures/carol-detail-url fixtures/carol-detail-page]
                      (let [expected [rating rating-count]]
                        (async/<!! (imdb/get-detail-page fixtures/carol-detail-url)) => expected)))
