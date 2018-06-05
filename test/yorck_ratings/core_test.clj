(ns yorck-ratings.core-test
  (:use org.httpkit.fake
        midje.sweet)
  (:require [yorck-ratings.core :refer :all]
            [yorck-ratings.fixtures :as fixtures]))

(fact "return rated movie infos sorted by rating"
      (with-fake-http [fixtures/yorck-list-url fixtures/yorck-list-page
                       fixtures/hateful-8-search-url fixtures/hateful-8-search-page
                       fixtures/hateful-8-detail-url fixtures/hateful-8-detail-page
                       fixtures/carol-search-url fixtures/carol-search-page
                       fixtures/carol-detail-url fixtures/carol-detail-page]
                      (let [expected [fixtures/hateful-8-rated-movie
                                      fixtures/carol-rated-movie]
                            actual (atom [])]
                        (rated-movies (fn [movies] (swap! actual concat movies)))
                        (Thread/sleep 500)
                        @actual => expected)))
