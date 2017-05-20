(ns yorck-ratings.web-sync-test
  (:use org.httpkit.fake
        hickory.core)
  (:require [clojure.test :refer :all]
            [yorck-ratings.web-sync :as web]
            [yorck-ratings.core-sync :as core]
            [clojure.java.io :as io]
            [hickory.select :as h]
            [clojure.string :as str]))

(defn load-fixture [filename]
  (->> filename
       (str "fixtures/")
       (io/resource)
       (slurp)))

(def yorck-list-fixture (load-fixture "yorck_list.html"))
(def hateful-8-dp-fixture (load-fixture "hateful_8_detail_page.html"))
(def hateful-8-sp-fixture (load-fixture "hateful_8_search_page.html"))
(def carol-dp-fixture (load-fixture "carol_detail_page.html"))
(def carol-sp-fixture (load-fixture "carol_search_page.html"))
(def yorck-list-url "https://www.yorck.de/filme?filter_today=true")

(deftest end-to-end-test
  (testing "return rated movie infos ordered by rating without sneak previews"
    (with-fake-http [yorck-list-url yorck-list-fixture
                     "https://m.imdb.com/find?q=The+Hateful+8" hateful-8-sp-fixture
                     "https://m.imdb.com/title/tt3460252/" hateful-8-dp-fixture
                     "https://m.imdb.com/find?q=Carol" carol-sp-fixture
                     "https://m.imdb.com/title/tt2402927/" carol-dp-fixture]
                    (let [hateful-8 "8.0 (123336) • <a href=\"https://m.imdb.com/title/tt3460252/\">The Hateful Eight</a> • <a href=\"https://www.yorck.de/filme/hateful-8-the\">The Hateful 8</a>"
                          carol "7.6 (22728) • <a href=\"https://m.imdb.com/title/tt2402927/\">Carol</a> • <a href=\"https://www.yorck.de/filme/carol\">Carol</a>"
                          html-str (:body (web/sync-handler ()))]
                      (is (str/includes? html-str hateful-8))
                      (is (str/includes? html-str carol))))))
