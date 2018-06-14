(ns yorck-ratings.imdb-test
  (:require [yorck-ratings.imdb :as imdb]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact => =not=> throws]]
            [yorck-ratings.fixtures :as fixtures]
            [clojure.core.async :as async]
            [yorck-ratings.rated-movie :as rated-movie]))

(def title fixtures/carol-imdb-title)
(def url fixtures/carol-detail-url)
(def rating fixtures/carol-rating)
(def rating-count fixtures/carol-rating-count)

(def default-rated-movie (rated-movie/from-yorck-info [fixtures/carol-yorck-title fixtures/carol-yorck-url]))

(def a-rated-movie-with-search-infos
  (rated-movie/with-imdb-info default-rated-movie [title url]))

(def a-rated-movie-with-detail-infos
  (rated-movie/with-imdb-rating a-rated-movie-with-search-infos [rating rating-count]))

(defn- make-get-search-infos-stub [infos]
  (fn [_]
    (let [result-chan (async/chan)]
      (async/put! result-chan infos)
      result-chan)))

(fact "adds imdb title and url"
      (let [result-chan (async/chan)]

        (imdb/search (make-get-search-infos-stub [title url]) default-rated-movie result-chan)

        (async/<!! result-chan) => a-rated-movie-with-search-infos))

(fact "does not add anything if the movie can't be found on imdb"
      (let [result-chan (async/chan)]

        (imdb/search (make-get-search-infos-stub []) default-rated-movie result-chan)

        (async/<!! result-chan) => default-rated-movie))

(fact "pulls titles and urls from imdb search page"
      (with-fake-routes-in-isolation
        {fixtures/carol-search-url (fixtures/status-ok fixtures/carol-search-page)}
        (let [yorck-title fixtures/carol-yorck-title]

          (async/<!! (imdb/get-search-page yorck-title)) => [title url])))

(fact "returns no search infos if the movie can not be found on imdb"
      (with-fake-routes-in-isolation
        {fixtures/no-search-result-url (fixtures/status-ok fixtures/no-search-result-search-page)}

        (async/<!! (imdb/get-search-page fixtures/no-search-result-yorck-title)) => []))

(defn- get-detail-page [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [rating rating-count])
    result-chan))

(fact "adds imdb rating and rating count"
      (let [result-chan (async/chan)]

        (imdb/detail get-detail-page a-rated-movie-with-search-infos result-chan)

        (async/<!! result-chan) => a-rated-movie-with-detail-infos))

(defn- never-called [_]
  (throw (UnsupportedOperationException. "get-detail-stub should not be called here")))

(fact "doesn't add detail info if it doesn't have search info"
      (let [result-chan (async/chan)]

        (imdb/detail never-called default-rated-movie result-chan) =not=> (throws UnsupportedOperationException)

        (async/<!! result-chan) => default-rated-movie))

(defn- get-detail-page-without-rating [_]
  (let [result-chan (async/chan)]
    (async/put! result-chan [])
    result-chan))

(fact "doesn't add rating if the movie doesn't have any"
      (let [result-chan (async/chan)]

        (imdb/detail get-detail-page-without-rating a-rated-movie-with-search-infos result-chan)

        (async/<!! result-chan) => a-rated-movie-with-search-infos))

(fact "pulls rating and rating count from imdb detail page"
      (with-fake-routes-in-isolation
        {url (fixtures/status-ok fixtures/carol-detail-page)}

        (async/<!! (imdb/get-detail-page url)) => [rating rating-count]))

(fact "return no rating if rating can't be found"
      (with-fake-routes-in-isolation
        {url (fixtures/status-ok fixtures/no-rating-detail-page)}

        (async/<!! (imdb/get-detail-page url)) => []))
