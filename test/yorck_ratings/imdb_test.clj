(ns yorck-ratings.imdb-test
  (:require [yorck-ratings.imdb :as imdb]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer [deftest is]]
            [yorck-ratings.fixtures :as fixtures]
            [yorck-ratings.rated-movie :as rated-movie]))

(def title fixtures/carol-imdb-title)
(def url fixtures/carol-detail-url)
(def rating fixtures/carol-rating)
(def rating-count fixtures/carol-rating-count)

(def default-rated-movie (rated-movie/from-yorck-info [fixtures/carol-yorck-title fixtures/carol-yorck-url]))

(def a-rated-movie-with-search-info
  (rated-movie/with-imdb-info default-rated-movie [title url]))

(def a-rated-movie-with-detail-info
  (rated-movie/with-imdb-rating a-rated-movie-with-search-info [rating rating-count]))

(defn- make-get-imdb-info-stub [info] (fn [_] info))

(deftest add-imdb-title-and-url
  (is (= (imdb/search (make-get-imdb-info-stub [title url]) default-rated-movie)
         a-rated-movie-with-search-info)))

(deftest without-movie-no-title
  (is (= (imdb/search (make-get-imdb-info-stub []) default-rated-movie)
         default-rated-movie)))

(deftest titles-and-urls-from-imdb-search-page
  (with-fake-routes-in-isolation
    {fixtures/carol-search-url (fixtures/status-ok fixtures/carol-search-page)}
    (let [yorck-title fixtures/carol-yorck-title]
      (is (= (imdb/get-search-page yorck-title)
             [title url])))))

(deftest without-movie-no-search-info
  (with-fake-routes-in-isolation
    {fixtures/no-search-result-url (fixtures/status-ok fixtures/no-search-result-search-page)}
    (is (= (imdb/get-search-page fixtures/no-search-result-yorck-title)
           []))))

(defn- get-detail-page [_] [rating rating-count])

(deftest add-imdb-rating-and-rating-count
  (is (= (imdb/detail get-detail-page a-rated-movie-with-search-info)
         a-rated-movie-with-detail-info)))

(def called (atom 0))

(defn- get-detail-page-without-rating [_] [])

(deftest no-rating-without-search-info
  (is (= (imdb/detail get-detail-page-without-rating a-rated-movie-with-search-info)
         a-rated-movie-with-search-info)))

(deftest rating-and-rating-count
  (with-fake-routes-in-isolation
    {url (fixtures/status-ok fixtures/carol-detail-page)}
    (is (= (imdb/get-detail-page url)
           [rating rating-count]))))

(deftest no-rating
  (with-fake-routes-in-isolation
    {url (fixtures/status-ok fixtures/no-rating-detail-page)}
    (is (= (imdb/get-detail-page url) []))))