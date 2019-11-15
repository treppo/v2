(ns cinema-ratings.cinema.yorck-test
  (:require [cinema-ratings.cinema.yorck :refer [info get-cinema-info]]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer [deftest is]]
            [clojure.spec.test.alpha :as spec-test]
            [cinema-ratings.rated-movie :as rated-movie]
            [cinema-ratings.fixtures :as fixtures]))

(spec-test/instrument `info)
(spec-test/instrument `get-cinema-info)

(def a-title "a title")
(def a-url "https://example.com")

(deftest rated-movies-with-yorck-info
  (let [get-page-stub (fn [] [{:title a-title :url a-url}])]

    (is (= (info get-page-stub) [(rated-movie/from-cinema-info {:title a-title :url a-url})]))))

(deftest remove-sneak-previews
  (let [get-page-stub (fn [] [{:title a-title :url a-url} {:title "a sneak preview" :url a-url}])]

    (is (= (info get-page-stub) [(rated-movie/from-cinema-info {:title a-title :url a-url})]))))

(deftest remove-premiere-from-title
  (let [get-page-stub (fn [] [{:title "My Zoe - Premiere" :url a-url}])]

    (is (= (info get-page-stub) [(rated-movie/from-cinema-info {:title "My Zoe" :url a-url})]))))

(defn without-dimension [title]
  (let [get-page-stub (fn [] [{:title title :url a-url}])]
    (info get-page-stub)))

(deftest remove-dimension-from-title
  (is (= (without-dimension "Pets - 2D")
         [(rated-movie/from-cinema-info {:title "Pets" :url a-url})]))

  (is (= (without-dimension "Ice Age - Kollision voraus! 2D!")
         [(rated-movie/from-cinema-info {:title "Ice Age - Kollision voraus!" :url a-url})])))

(defn- with-rotated-article [title]
  (let [get-page-stub (fn [] [{:title title :url a-url}])]
    (info get-page-stub)))

(deftest rotate-articles
  (is (= (with-rotated-article "Hateful 8, The")
         [(rated-movie/from-cinema-info {:title "The Hateful 8" :url a-url})]))

  (is (= (with-rotated-article "Hail, Caesar!")
         [(rated-movie/from-cinema-info {:title "Hail, Caesar!" :url a-url})]))

  (is (= (with-rotated-article "Brandneue Testament, Das")
         [(rated-movie/from-cinema-info {:title "Das Brandneue Testament" :url a-url})]))

  (is (= (with-rotated-article "Unterhändler, Der")
         [(rated-movie/from-cinema-info {:title "Der Unterhändler" :url a-url})]))

  (is (= (with-rotated-article "Winzlinge, Die - Operation Zuckerdose")
         [(rated-movie/from-cinema-info {:title "Die Winzlinge - Operation Zuckerdose" :url a-url})])))

(deftest pull-titles-and-urls-from-yorck-page
  (with-fake-routes-in-isolation
    {fixtures/yorck-list-url (fixtures/yorck-list-ok)}
    (let [expected [{:title fixtures/carol-yorck-title :url fixtures/carol-yorck-url}
                    {:title fixtures/hateful-8-yorck-unfiltered-title :url fixtures/hateful-8-yorck-url}
                    {:title "Sneak FAF" :url "https://www.yorck.de/filme/sneak-faf"}]]
      (is (= (get-cinema-info)
             expected)))))
