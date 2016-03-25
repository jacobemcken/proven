(ns proven.validator-test
  (:require [clojure.test :refer :all]
            [proven.validator :refer :all]
            [proven.rule :as rule]))

(defn always-fails
  [keys & [msg]]
  (rule/make-validator keys (fn [_] true) (or msg "always fails validation")))

(deftest apply-path-test
  (testing "Apply path (prefix) on keys"
    (are [input-path input-keys output] (= (apply-path input-path input-keys) output)
      [] #{[:key1] [:key2]} #{[:key1] [:key2]}
      [:nest1] #{[:key1]} #{[:nest1 :key1]}
      [:nest1] #{[:key1] [:key2]} #{[:nest1 :key1] [:nest1 :key2]}
      [:nest1 :nest2] #{[:key1] [:key2]} #{[:nest1 :nest2 :key1] [:nest1 :nest2 :key2]})))

(deftest apply-rule-test
  (testing "Apply path to all validation errors"
    (are [input path affected-paths] (= (get-in (apply-rule (rule/required [:key]) path input) [0 :paths]))
      {:key "some string"} [:nested :path] nil
      {:key ""} [:nested :path] #{[:nested :path :key]}
      {:key ""} [:path] #{[:path :key]}
      )))

(deftest validate-test
  (testing "Validate rule set"
    (let [rules (vector
                 (always-fails [:key1])
                 (always-fails [:key2] "my message")
                 (always-fails [:key3]))]
      (is (true? (= 3 (count (validate rules {}))))))))
