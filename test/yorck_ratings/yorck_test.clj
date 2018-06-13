(ns yorck-ratings.yorck-test
  (:require [yorck-ratings.yorck :as yorck]
            [clj-http.fake :refer [with-fake-routes-in-isolation]]
            [midje.sweet :refer [fact =>]]
            [clojure.core.async :as async]
            [yorck-ratings.rated-movie :as rated-movie]
            [yorck-ratings.fixtures :as fixtures]))

(def a-url "a url")
(defn- make-get-page-stub [yorck-infos]
  (fn [] (async/to-chan [yorck-infos])))

(fact "returns rated-movies with yorck title and yorck url"
      (let [get-page-stub (make-get-page-stub [["a title" a-url]])
            result-channel (async/chan)
            _ (yorck/infos get-page-stub result-channel)]
        (async/<!! result-channel) => [(rated-movie/make {:yorck-title "a title"
                                                          :yorck-url   a-url})]))

(fact "removes sneak previews"
      (let [get-page-stub (make-get-page-stub [["a title" a-url] ["a sneak preview" "another url"]])
            result-channel (async/chan)
            _ (yorck/infos get-page-stub result-channel)]
        (async/<!! result-channel) => [(rated-movie/make {:yorck-title "a title"
                                                          :yorck-url   a-url})]))

(defn without-dimension [title]
  (let [get-page-stub (make-get-page-stub [[title a-url]])
        result-channel (async/chan)]
    (yorck/infos get-page-stub result-channel)
    (async/<!! result-channel)))

(fact "removes dimension from title"
      (without-dimension "Pets - 2D") =>
      [(rated-movie/make {:yorck-title "Pets"
                          :yorck-url   a-url})]

      (without-dimension "Ice Age - Kollision voraus! 2D!") =>
      [(rated-movie/make {:yorck-title "Ice Age - Kollision voraus!"
                          :yorck-url   a-url})])

(defn- with-rotated-article [title]
  (let [get-page-stub (make-get-page-stub [[title a-url]])
        result-channel (async/chan)]
    (yorck/infos get-page-stub result-channel)
    (async/<!! result-channel)))

(fact "rotates articles"
      (with-rotated-article "Hateful 8, The") =>
      [(rated-movie/make {:yorck-title "The Hateful 8"
                          :yorck-url   a-url})]

      (with-rotated-article "Hail, Caesar!") =>
      [(rated-movie/make {:yorck-title "Hail, Caesar!"
                          :yorck-url   a-url})]

      (with-rotated-article "Brandneue Testament, Das") =>
      [(rated-movie/make {:yorck-title "Das Brandneue Testament"
                          :yorck-url   a-url})]

      (with-rotated-article "Unterhändler, Der") =>
      [(rated-movie/make {:yorck-title "Der Unterhändler"
                          :yorck-url   a-url})]

      (with-rotated-article "Winzlinge, Die - Operation Zuckerdose") =>
      [(rated-movie/make {:yorck-title "Die Winzlinge - Operation Zuckerdose"
                          :yorck-url   a-url})])

(fact "pulls titles and urls from yorck page"
      (with-fake-routes-in-isolation
        {fixtures/yorck-list-url (fn [request] {:status 200 :headers {} :body fixtures/yorck-list-page})}
        (let [expected [[fixtures/carol-yorck-title fixtures/carol-yorck-url]
                        [fixtures/hateful-8-yorck-title fixtures/hateful-8-yorck-url]
                        ["Sneak FAF" "https://www.yorck.de/filme/sneak-faf"]]]
          (async/<!! (yorck/get-yorck-infos-async)) => expected)))
