(ns yorck-ratings.imdb-test
  (:require [yorck-ratings.imdb :as imdb]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact => =not=> throws]]
            [yorck-ratings.fixtures :as fixtures]
            [clojure.core.async :as async]
            [yorck-ratings.rated-movie :as rated-movie]))

(def title fixtures/carol-imdb-title)
(def default-rated-movie (rated-movie/make {}))
(def a-rated-movie-with-search-infos (rated-movie/make {:imdb-url fixtures/carol-detail-url :imdb-title title}))

(def url fixtures/carol-detail-url)
(def rating fixtures/carol-rating)
(def rating-count fixtures/carol-rating-count)

(defn- make-get-search-infos-stub [infos]
  (fn [_]
    (let [result-chan (async/chan)]
      (async/put! result-chan infos)
      result-chan)))

(fact "adds imdb title and url"
      (let [result-chan (async/chan)]

        (imdb/search (make-get-search-infos-stub [title url]) default-rated-movie result-chan)

        (async/<!! result-chan) => (rated-movie/make {:imdb-title title
                                                      :imdb-url   url})))

(fact "does not add anything if the movie can't be found on imdb"
      (let [result-chan (async/chan)]

        (imdb/search (make-get-search-infos-stub []) default-rated-movie result-chan)

        (async/<!! result-chan) => (rated-movie/make {})))

(fact "pulls titles and urls from imdb search page"
      (with-fake-routes-in-isolation
        {fixtures/carol-search-url (fixtures/status-ok fixtures/carol-search-page)}
        (let [yorck-title fixtures/carol-yorck-title]

          (async/<!! (imdb/get-search-page yorck-title)) => [title url])))

(fact "returns no search infos if the movie can not be found on imdb"
      (with-fake-routes-in-isolation
        {fixtures/no-search-result-url (fixtures/status-ok fixtures/no-search-result-search-page)}

        (async/<!! (imdb/get-search-page fixtures/no-search-result-yorck-title)) => []))

(defn- get-detail-page-stub [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [rating rating-count])
    result-chan))

(fact "adds imdb rating and rating count"
      (let [result-chan (async/chan)]

        (imdb/detail get-detail-page-stub a-rated-movie-with-search-infos result-chan)

        (async/<!! result-chan) => (rated-movie/make {:rating       rating
                                                      :rating-count rating-count
                                                      :imdb-url     url
                                                      :imdb-title   title})))

(defn- never-called-stub [_]
  (throw (UnsupportedOperationException. "get-detail-stub should not be called here")))

(fact "doesn't add detail info if it doesn't have search info"
      (let [result-chan (async/chan)]

        (imdb/detail never-called-stub default-rated-movie result-chan) =not=> (throws UnsupportedOperationException)

        (async/<!! result-chan) => default-rated-movie))

(fact "pulls rating and rating count from imdb detail page"
      (with-fake-routes-in-isolation
        {url (fixtures/status-ok fixtures/carol-detail-page)}
        (let [expected [rating rating-count]]

          (async/<!! (imdb/get-detail-page url)) => expected)))
