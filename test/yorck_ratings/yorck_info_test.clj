(ns yorck-ratings.yorck-info-test
  (:use org.httpkit.fake
        hickory.core)
  (:require [clojure.test :refer :all]
            [yorck-ratings.yorck-info :as yorck-info]
            [clojure.java.io :as io]
            [clojure.core.async :as a]))

(defn load-fixture [filename]
  (->> filename
       (str "fixtures/")
       (io/resource)
       (slurp)))

(def yorck-list-fixture (load-fixture "yorck_list.html"))
(def parsed-yorck-list-fixture (as-hickory (parse yorck-list-fixture)))

(deftest get-yorck-info-test
  (testing "fetches info for all yorck movies without sneak previews"
    (let [expected [{:yorck-title "Carol"
                     :yorck-url   "https://www.yorck.de/filme/carol"}
                    {:yorck-title "The Hateful 8"
                     :yorck-url   "https://www.yorck.de/filme/hateful-8-the"}]
          actual (yorck-info/get-info (fn [url] parsed-yorck-list-fixture))]
      (is (= actual expected)))))

(deftest rotate-article-test
  (testing "fixes Yorck titles with their article at the end"
    (is (= "The Hateful 8" (yorck-info/rotate-article "Hateful 8, The")))
    (is (= "Hail, Caesar!" (yorck-info/rotate-article "Hail, Caesar!")))
    (is (= "Das Brandneue Testament" (yorck-info/rotate-article "Brandneue Testament, Das")))
    (is (= "Der Unterhändler" (yorck-info/rotate-article "Unterhändler, Der")))
    (is (= "Die Winzlinge - Operation Zuckerdose" (yorck-info/rotate-article "Winzlinge, Die - Operation Zuckerdose"))))

  (testing "leaves titles without article untouched"
    (is (= "Carol" (yorck-info/rotate-article "Carol")))))

(deftest remove-dimension-test
  (testing "fixes Yorck titles with 2D info added"
    (is (= "Pets" (yorck-info/remove-dimension "Pets - 2D")))
    (is (= "Ice Age - Kollision voraus!" (yorck-info/remove-dimension "Ice Age - Kollision voraus! 2D!"))))

  (testing "leaves titles without 2D info untouched"
    (is (= "Lou Andreas-Salomé" (yorck-info/remove-dimension "Lou Andreas-Salomé")))))
