(ns yorck-ratings.core-test
  (:require [yorck-ratings.core :refer :all]
            [yorck-ratings.fixtures :as fixtures]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact =>]]))

(fact "return rated movie infos sorted by rating"
      (with-fake-routes-in-isolation
        {fixtures/yorck-list-url       (fn [request] {:status 200 :headers {} :body fixtures/yorck-list-page})
         fixtures/hateful-8-search-url (fn [request] {:status 200 :headers {} :body fixtures/hateful-8-search-page})
         fixtures/hateful-8-detail-url (fn [request] {:status 200 :headers {} :body fixtures/hateful-8-detail-page})
         fixtures/carol-search-url     (fn [request] {:status 200 :headers {} :body fixtures/carol-search-page})
         fixtures/carol-detail-url     (fn [request] {:status 200 :headers {} :body fixtures/carol-detail-page})}
        (let [expected [fixtures/hateful-8-rated-movie
                        fixtures/carol-rated-movie]
              actual (atom [])]
          (rated-movies (fn [movies] (swap! actual concat movies)))
          (Thread/sleep 500)
          @actual => expected)))
