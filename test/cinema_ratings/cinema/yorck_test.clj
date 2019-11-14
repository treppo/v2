(ns cinema-ratings.cinema.yorck-test
  (:require [cinema-ratings.cinema.yorck :as yorck]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [clojure.test :refer [deftest is]]
            [cinema-ratings.rated-movie :as rated-movie]
            [cinema-ratings.fixtures :as fixtures]))

(def a-title "a title")
(def a-url "https://example.com")

(deftest rated-movies-with-yorck-info
  (let [get-page-stub (fn [] [[a-title a-url]])]

    (is (= (yorck/info get-page-stub) [(rated-movie/from-cinema-info [a-title a-url])]))))

(deftest remove-sneak-previews
  (let [get-page-stub (fn [] [[a-title a-url] ["a sneak preview" "another url"]])]

    (is (= (yorck/info get-page-stub) [(rated-movie/from-cinema-info [a-title a-url])]))))

(deftest remove-premiere-from-title
  (let [get-page-stub (fn [] [["My Zoe - Premiere" a-url]])]

    (is (= (yorck/info get-page-stub) [(rated-movie/from-cinema-info ["My Zoe" a-url])]))))

(defn without-dimension [title]
  (let [get-page-stub (fn [] [[title a-url]])]
    (yorck/info get-page-stub)))

(deftest remove-dimension-from-title
  (is (= (without-dimension "Pets - 2D")
         [(rated-movie/from-cinema-info ["Pets" a-url])]))

  (is (= (without-dimension "Ice Age - Kollision voraus! 2D!")
         [(rated-movie/from-cinema-info ["Ice Age - Kollision voraus!" a-url])])))

(defn- with-rotated-article [title]
  (let [get-page-stub (fn [] [[title a-url]])]
    (yorck/info get-page-stub)))

(deftest rotate-articles
  (is (= (with-rotated-article "Hateful 8, The")
         [(rated-movie/from-cinema-info ["The Hateful 8" a-url])]))

  (is (= (with-rotated-article "Hail, Caesar!")
         [(rated-movie/from-cinema-info ["Hail, Caesar!" a-url])]))

  (is (= (with-rotated-article "Brandneue Testament, Das")
         [(rated-movie/from-cinema-info ["Das Brandneue Testament" a-url])]))

  (is (= (with-rotated-article "Unterhändler, Der")
         [(rated-movie/from-cinema-info ["Der Unterhändler" a-url])]))

  (is (= (with-rotated-article "Winzlinge, Die - Operation Zuckerdose")
         [(rated-movie/from-cinema-info ["Die Winzlinge - Operation Zuckerdose" a-url])])))

(deftest pull-titles-and-urls-from-yorck-page
  (with-fake-routes-in-isolation
    {fixtures/yorck-list-url (fixtures/yorck-list-ok)}
    (let [expected [[fixtures/carol-yorck-title fixtures/carol-yorck-url]
                    [fixtures/hateful-8-yorck-unfiltered-title fixtures/hateful-8-yorck-url]
                    ["Sneak FAF" "https://www.yorck.de/filme/sneak-faf"]]]
      (is (= (yorck/get-cinema-info)
             expected)))))
