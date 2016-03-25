(ns proven.core-test
  (:require [clojure.test :refer :all]
            [proven.core :refer [seqify]])
  (:import [proven.core Err]))

(deftest seqify-test
  (testing "Seqification"
    (are [input should-seqify?] (let [output (seqify input)]
                                  (if should-seqify?
                                    (not= input output)
                                    (= input output)))
      :keyword true
      "string" true
      #{:keyword} false
      [:keyword1 :keyword1] false)))
