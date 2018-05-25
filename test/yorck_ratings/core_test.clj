(ns yorck-ratings.core-test
  (:use org.httpkit.fake
        hickory.core)
  (:require [clojure.test :refer :all]
            [yorck-ratings.core :refer :all]
            [clojure.java.io :as io]
            [clojure.core.async :as a]))

(defn load-fixture [filename]
  (->> filename
       (str "fixtures/")
       (io/resource)
       (slurp)))

(def yorck-list-fixture (load-fixture "yorck_list.html"))
(def parsed-yorck-list-fixture (as-hickory (parse yorck-list-fixture)))
(def hateful-8-dp-fixture (load-fixture "hateful_8_detail_page.html"))
(def hateful-8-sp-fixture (load-fixture "hateful_8_search_page.html"))
(def parsed-hateful-8-sp-fixture (as-hickory (parse hateful-8-sp-fixture)))
(def parsed-hateful-8-dp-fixture (as-hickory (parse hateful-8-dp-fixture)))
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
                    (let [expected [(make-rated-movie {:rating       7.8
                                                       :rating-count 391351
                                                       :imdb-title   "The Hateful Eight"
                                                       :imdb-url     "https://m.imdb.com/title/tt3460252/"
                                                       :yorck-title  "The Hateful 8"
                                                       :yorck-url    "https://www.yorck.de/filme/hateful-8-the"})
                                    (make-rated-movie {:rating       7.2
                                                       :rating-count 89891
                                                       :imdb-title   "Carol"
                                                       :imdb-url     "https://m.imdb.com/title/tt2402927/"
                                                       :yorck-title  "Carol"
                                                       :yorck-url    "https://www.yorck.de/filme/carol"})]
                          actual (atom [])]
                      (rated-movies (fn [movies] (swap! actual concat movies)))
                      (Thread/sleep 500)
                      (is (= @actual expected))))))

(deftest get-async-test
  (testing "writes parsed successful get request result to channel"
    (with-fake-http [yorck-list-url yorck-list-fixture]
                    (let [result-ch (get-async yorck-list-url)]
                      (is (= parsed-yorck-list-fixture (a/<!! result-ch)))))))

(deftest yorck-titles-test
  (testing "returns yorck movie titles"
    (let [expected ["Carol" "The Hateful 8" "Sneak FAF"]]
      (is (= expected (yorck-titles parsed-yorck-list-fixture))))))

(deftest yorck-urls-test
  (testing "returns yorck movie urls"
    (let [expected ["https://www.yorck.de/filme/carol"
                    "https://www.yorck.de/filme/hateful-8-the"
                    "https://www.yorck.de/filme/sneak-faf"]]
      (is (= expected (yorck-urls parsed-yorck-list-fixture))))))

(deftest yorck-sp-infos-test
  (testing "returns yorck movie titles and urls"
    (let [expected [(make-rated-movie {:yorck-title "Carol"
                                       :yorck-url   "https://www.yorck.de/filme/carol"})
                    (make-rated-movie {:yorck-title "The Hateful 8"
                                       :yorck-url   "https://www.yorck.de/filme/hateful-8-the"})
                    (make-rated-movie {:yorck-title "Sneak FAF"
                                       :yorck-url   "https://www.yorck.de/filme/sneak-faf"})]]
      (is (= expected (yorck-titles-urls parsed-yorck-list-fixture))))))

(deftest imdb-titles-test
  (testing "returns imdb movie titles"
    (let [expected "The Hateful Eight"]
      (is (= expected (imdb-title parsed-hateful-8-sp-fixture))))))

(deftest imdb-urls-test
  (testing "returns imdb movie urls with parameters"
    (let [expected "https://m.imdb.com/title/tt3460252/"]
      (is (= expected (imdb-url parsed-hateful-8-sp-fixture))))))

(deftest imdb-titles-urls-test
  (testing "returns imdb movie titles"
    (let [movie (make-rated-movie {:yorck-title "Carol"
                                    :yorck-url   "https://www.yorck.de/filme/carol"})
          expected (make-rated-movie {:yorck-title "Carol"
                                      :yorck-url   "https://www.yorck.de/filme/carol"
                                      :imdb-title  "The Hateful Eight"
                                      :imdb-url    "https://m.imdb.com/title/tt3460252/"})]
      (is (= expected (with-imdb-search-infos movie parsed-hateful-8-sp-fixture))))))

(deftest imdb-rating-test
  (testing "returns imdb movie rating"
    (let [expected 7.8]
      (is (= expected (imdb-rating parsed-hateful-8-dp-fixture))))))

(deftest imdb-rating-count-test
  (testing "returns imdb movie rating count"
    (let [expected 391351]
      (is (= expected (imdb-rating-count parsed-hateful-8-dp-fixture))))))

(deftest rotate-article-test
  (testing "fixes Yorck titles with their article at the end"
    (is (= "The Hateful 8" (rotate-article "Hateful 8, The")))
    (is (= "Hail, Caesar!" (rotate-article "Hail, Caesar!")))
    (is (= "Das Brandneue Testament" (rotate-article "Brandneue Testament, Das")))
    (is (= "Der Unterhändler" (rotate-article "Unterhändler, Der")))
    (is (= "Die Winzlinge - Operation Zuckerdose" (rotate-article "Winzlinge, Die - Operation Zuckerdose"))))

  (testing "leaves titles without article untouched"
    (is (= "Carol" (rotate-article "Carol")))))

(deftest remove-dimension-test
  (testing "fixes Yorck titles with 2D info added"
    (is (= "Pets" (remove-dimension "Pets - 2D")))
    (is (= "Ice Age - Kollision voraus!" (remove-dimension "Ice Age - Kollision voraus! 2D!"))))

  (testing "leaves titles without 2D info untouched"
    (is (= "Lou Andreas-Salomé" (remove-dimension "Lou Andreas-Salomé")))))
