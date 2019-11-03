(ns yorck-ratings.cache-test
  (:require [clojure.test :refer :all])
  (:require [yorck-ratings.cache :refer :all]))

(deftest cache-daily-test
  (is (nil? (from-cache)))

  (is (= (into-cache :cached-value) :cached-value))

  (is (= (from-cache) :cached-value))

  (reset)

  (is (nil? (from-cache))))
