(ns yorck-ratings.fixtures
  (:require [clojure.java.io :as io]
            [yorck-ratings.rated-movie :as rated-movie]))

(defn- load-fixture [filename]
  (->> filename
       (str "fixtures/")
       (io/resource)
       (slurp)))

(defn status-ok [body]
  (fn [request] {:status 200 :headers {} :body body}))

(def yorck-list-url "https://www.yorck.de/filme?filter_today=true")
(def yorck-list-page (load-fixture "yorck_list.html"))
(defn yorck-list-ok []
  (fn [request] {:status 200 :headers {} :body yorck-list-page}))


(def no-search-result-yorck-title "No search result")
(def no-search-result-search-page (load-fixture "no_search_result_search_page.html"))
(def no-search-result-url "https://m.imdb.com/find?q=No+search+result")

(def carol-yorck-title "Carol")
(def carol-yorck-url "https://www.yorck.de/filme/carol")
(def carol-search-page (load-fixture "carol_search_page.html"))
(def carol-search-url "https://m.imdb.com/find?q=Carol")
(def carol-detail-page (load-fixture "carol_detail_page.html"))
(def carol-detail-url "https://m.imdb.com/title/tt2402927/")
(def carol-imdb-title "Carol")
(def carol-rating 7.2)
(def carol-rating-count 89891)
(def carol-rated-movie (rated-movie/make {:rating       7.2
                                          :rating-count 89891
                                          :imdb-title   "Carol"
                                          :imdb-url     carol-detail-url
                                          :yorck-title  carol-yorck-title
                                          :yorck-url    carol-yorck-url}))

(def hateful-8-yorck-title "Hateful 8, The")
(def hateful-8-yorck-url "https://www.yorck.de/filme/hateful-8-the")
(def hateful-8-search-url "https://m.imdb.com/find?q=The+Hateful+8")
(def hateful-8-detail-url "https://m.imdb.com/title/tt3460252/")
(def hateful-8-detail-page (load-fixture "hateful_8_detail_page.html"))
(def hateful-8-search-page (load-fixture "hateful_8_search_page.html"))

(def hateful-8-rated-movie (rated-movie/make {:rating       7.8
                                              :rating-count 391351
                                              :imdb-title   "The Hateful Eight"
                                              :imdb-url     hateful-8-detail-url
                                              :yorck-title  "The Hateful 8"
                                              :yorck-url    hateful-8-yorck-url}))
