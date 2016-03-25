(ns proven.rule
  "Contains helper functions to build rules"
  (:require [clojure.string :as string]
            [proven.core :refer [seqify #?(:cljs Err)]])
  #?(:clj (:import [proven.core Err])))

(defn make-validator
  "Takes a set of keys related to the predicate f along with an error message.
Returns a validator which returns a list of errors (defrecord Err) when
validation fails or nil otherwise.

Error example:
'(proven.core.Err{
    :paths #{[:key1 :key2] [:key3]},
    :msg  \"error message\"
  })"
  [keys pred msg]
  (fn [data]
    (when-let [affected-keys
               (->>
                (seqify keys)
                (filter #(pred (get data % ::absent)))
                not-empty)]
      ;; convert affected keys to relative paths (by wrapping them in a vector)
      (list (Err. (into (hash-set) (map vector affected-keys)) msg)))))

(defn enforce-set
  [coll]
  (if (set? coll)
    coll
    (set coll)))

;; Validation rule builder helpers

(defn blank-string?
  [s]
  (and (string? s) (string/blank? s)))

(defn when-present
  "When the value is present (not absent or nil) fail (return true)
if the function f returns false."
  [f]
  (fn [value]
    (and (not= ::absent value)
         (not (nil? value))
         (not (f value)))))

(defn contains
  "The keys must be present in the map but may be blank."
  [keys & [msg]]
  (make-validator keys #{::absent}
                  (or msg "must be present")))

(defn required
  "The keys must be present and not be blank."
  [keys & [msg]]
  (make-validator keys #(or (= ::absent %) (nil? %) (blank-string? %))
                  (or msg "must not be blank or missing")))

(defn not-blank
  "If present, the keys must not be blank."
  [keys & [msg]]
  (make-validator keys #(or (nil? %) (blank-string? %))
                  (or msg "must not be blank")))

(defn exact [val keys & [msg]]
  (make-validator keys #(and (not= ::absent %) (not= val %))
                  (or msg "incorrect value")))

(defn matches [re keys & [msg]]
  "Takes a regular expression and the keys for which values must match when the
key exists and the values isn't blank."
  (make-validator keys #(and (not= ::absent %)
                             (not (string/blank? %))
                             (not (re-matches re %)))
                  (or msg "incorrect format")))

(defn min-length [len keys & [msg]]
  (make-validator keys (when-present #(<= len (count %)))
                  (or msg (str "must be at least " len " characters"))))

(defn max-length [len keys & [msg]]
  (make-validator keys (when-present #(>= len (count %)))
                  (or msg (str "cannot exceed " len " characters"))))

(defn exact-length [len keys & [msg]]
  (make-validator keys (when-present #(= len (count %)))
                  (or msg (str "length must be " len))))

(defn between-length [[lower upper] keys & [msg]]
  (make-validator keys (when-present #(<= lower (count %) upper))
                  (or msg (str "must be between " lower " and " upper " characters"))))

(defn combine [& validators]
  (fn [m]
    (apply concat (map seqify (keep #(% m) validators)))))

(defn in [coll keys & [msg]]
  (make-validator keys (when-present #(contains? (enforce-set coll) %))
                  (or msg (str "not an accepted value"))))

(defn every-in [coll keys & [msg]]
  (make-validator keys (when-present #(every? (fn [x] (contains? (enforce-set coll) x)) %))
                  (or msg (str "not an accepted value"))))
