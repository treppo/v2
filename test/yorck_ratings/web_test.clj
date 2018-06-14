(ns yorck-ratings.web-test
  (:require [yorck-ratings.web :refer [async-handler]]
            [yorck-ratings.fixtures :as fixtures]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact => contains]]
            [hickory.core :as hickory]
            [hickory.render :as render]
            [hickory.select :as selector]
            [hiccup.core :as hiccup]
            [ring.mock.request :as mock]
            [yorck-ratings.view :as view]))

(defn- get-rated-movies-html [page-html]
  (->> page-html
       (hickory/parse)
       (hickory/as-hickory)
       (selector/select (selector/child (selector/class :rated-movie)))
       (mapv render/hickory-to-html)))

(fact "return rated movie infos sorted by rating"
      (with-fake-routes-in-isolation
        {fixtures/yorck-list-url       (fixtures/yorck-list-ok)
         fixtures/hateful-8-search-url (fixtures/status-ok fixtures/hateful-8-search-page)
         fixtures/hateful-8-detail-url (fixtures/status-ok fixtures/hateful-8-detail-page)
         fixtures/carol-search-url     (fixtures/status-ok fixtures/carol-search-page)
         fixtures/carol-detail-url     (fixtures/status-ok fixtures/carol-detail-page)}
        (let [expected [(hiccup/html (view/movie-item fixtures/hateful-8-rated-movie))
                        (hiccup/html (view/movie-item fixtures/carol-rated-movie))]
              response (atom {})
              success-handler (fn [actual] (swap! response merge actual))
              error-handler identity]

          (async-handler (mock/request :get "/") success-handler error-handler)
          (Thread/sleep 500)

          (:status @response) => 200
          (get-rated-movies-html (:body @response)) => expected)))
