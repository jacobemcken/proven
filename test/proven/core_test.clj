(ns proven.core-test
  (:require [clojure.test :refer :all]
            [proven.core :refer :all]))

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

(deftest enforce-seq-test
  (testing "Enforcing datastructure being wrapped in a list if not already a seq"
    (let [error [#{[:key]}]]
      (are [input output] (= (enforce-seq input) output)
        error (list error)
        (list error) (list error)
        (lazy-seq error) (lazy-seq error) ;lists are converted to lazy-seq when passing through map
        nil nil))))

(deftest apply-path-test
  (testing "Apply path (prefix) on keys"
    (are [input-path input-keys output] (= (apply-path input-path input-keys) output)
      [] #{:key1 :key2} #{[:key1] [:key2]}
      [:nest1] #{:key1} #{[:nest1 :key1]}
      [:nest1] #{:key1 :key2} #{[:nest1 :key1] [:nest1 :key2]}
      [:nest1 :nest2] #{:key1 :key2} #{[:nest1 :nest2 :key1] [:nest1 :nest2 :key2]})))

(deftest check-rule-test
  (testing "Apply path to all validation errors"
    (are [input path affected-paths] (= (map first (check-rule (required [:key]) path input)) )
      {:key "some string"} [:nested :path] nil
      {:key ""} [:nested :path] #{[:nested :path :key]}
      {:key ""} [:path] #{[:path :key]}
      )))

(deftest required-test
  (testing "Required rule generator"
    (let [rule (required #{:key})]
      (are [input validates?] (let [rule-result (rule input)]
                                (if validates?
                                  (nil? rule-result)
                                  (not (empty? rule-result))))
        {:other-key ""} false
        {:key nil}      false
        {:key ""}       false
        {:key "non black string"} true))))

(deftest not-blank-test
  (testing "Not blank rule generator"
    (let [rule (not-blank #{:key})]
      (are [input validates?] (let [rule-result (rule input)]
                                (if validates?
                                  (nil? rule-result)
                                  (not (empty? rule-result))))
        {:other-key ""} true
        {:key nil}      false
        {:key ""}       false
        {:key "non black string"} true))))

(deftest matches-test
  (testing "Rule builder helper for regular expressions"
    (let [rule (matches #"[0-9]{4}" #{:key})]
      (are [input should-validate?] (let [validates? (empty? (rule input))]
                                      (= should-validate? validates?))
        {:other-key ""} true
        {:key nil}      true
        {:key ""}       true
        {:key "123"}    false
        {:key "1234"}   true
        {:key "12345"}  false))))

(deftest min-length-test
  (testing "Rule builder helper for minimum length on string"
    (let [rule (min-length 3 #{:key})]
      (are [input should-validate?] (let [validates? (empty? (rule input))]
                                      (= should-validate? validates?))
        {:other-key ""} true
        {:key nil}      true
        {:key ""}       false
        {:key "12"}     false
        {:key "123"}    true
        {:key "1235"}   true))))

(deftest max-length-test
  (testing "Rule builder helper for maximum length on string"
    (let [rule (max-length 3 #{:key})]
      (are [input should-validate?] (let [validates? (empty? (rule input))]
                                      (= should-validate? validates?))
        {:other-key ""} true
        {:key nil}      true
        {:key ""}       true
        {:key "12"}     true
        {:key "123"}    true
        {:key "1235"}   false))))

(deftest between-length-test
  (testing "Rule builder helper for length between two numbers on string"
    (let [rule (between-length [3 5] #{:key})]
      (are [input should-validate?] (let [validates? (empty? (rule input))]
                                      (= should-validate? validates?))
        {:other-key ""} true
        {:key nil}      true
        {:key ""}       false
        {:key "12"}     false
        {:key "123"}    true
        {:key "1234"}   true
        {:key "12345"}  true
        {:key "123456"} false))))
