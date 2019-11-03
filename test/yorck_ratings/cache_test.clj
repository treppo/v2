(ns yorck-ratings.cache-test
  (:require [clojure.test :refer :all])
  (:require [yorck-ratings.cache :refer :all])
  (:import (java.time Clock Duration)))

(defn tomorrow-clock [] (Clock/offset (Clock/systemUTC) (Duration/ofDays 1)))

(use-fixtures :each (fn [test-f]
                      (test-f)
                      (reset)))

(deftest cache-test
  (is (nil? (from-cache)))

  (is (= (into-cache :cached-value) :cached-value))

  (is (= (from-cache) :cached-value)))

(deftest cache-expiry-test
  (is (= (into-cache :cached-value) :cached-value))

  (binding [clock (tomorrow-clock)]
    (is (nil? (from-cache)))))
