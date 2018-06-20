(ns yorck-ratings.imdb-test
  (:require [yorck-ratings.imdb :as imdb]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact => =not=> throws]]
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

(fact "adds imdb title and url"
      (imdb/search (make-get-imdb-info-stub [title url]) default-rated-movie) => a-rated-movie-with-search-info)

(fact "does not add anything if the movie can't be found on imdb"
      (imdb/search (make-get-imdb-info-stub []) default-rated-movie) => default-rated-movie)

(fact "pulls titles and urls from imdb search page"
      (with-fake-routes-in-isolation
        {fixtures/carol-search-url (fixtures/status-ok fixtures/carol-search-page)}
        (let [yorck-title fixtures/carol-yorck-title]

          (imdb/get-search-page yorck-title) => [title url])))

(fact "returns no search info if the movie can not be found on imdb"
      (with-fake-routes-in-isolation
        {fixtures/no-search-result-url (fixtures/status-ok fixtures/no-search-result-search-page)}

        (imdb/get-search-page fixtures/no-search-result-yorck-title) => []))

(defn- get-detail-page [_] [rating rating-count])

(fact "adds imdb rating and rating count"
      (imdb/detail get-detail-page a-rated-movie-with-search-info) => a-rated-movie-with-detail-info)

(defn- get-detail-page-without-rating [_] [])

(fact "doesn't add rating if the movie doesn't have any"
      (imdb/detail get-detail-page-without-rating a-rated-movie-with-search-info) => a-rated-movie-with-search-info)

(fact "pulls rating and rating count from imdb detail page"
      (with-fake-routes-in-isolation
        {url (fixtures/status-ok fixtures/carol-detail-page)}

        (imdb/get-detail-page url) => [rating rating-count]))

(fact "return no rating if rating can't be found"
      (with-fake-routes-in-isolation
        {url (fixtures/status-ok fixtures/no-rating-detail-page)}

        (imdb/get-detail-page url) => []))
