(ns proven.core
  (:require [clojure.string :as string]))

(defn seqify [x]
  (if-not (or (sequential? x) (set? x)) [x] x))

(defn make-validator
  [keys f msg]
  (fn [data]
    (when-let [affected-keys
               (->>
                (seqify keys)
                (filter #(f (get data % ::absent)))
                not-empty)]
      [(into (hash-set) affected-keys) msg])))

(defn enforce-seq
  [data]
  (when data
    (if (seq? data) data (list data))))

(defn enforce-set
  [coll]
  (if (set? coll)
    coll
    (set coll)))

(defn apply-path
  "Prefix the keys with given path within validation result"
  [path ks]
  (into (hash-set) (map #(concat path (enforce-seq %)) ks)))

(defn check-rule
  [f path data]
  (when-let [errors (enforce-seq (f data))]
    (map #(update % 0 (partial apply-path path)) errors)))

(defn validate
  ([rules]
   (validate rules []))
  ([rules path]
   (fn [data]
     (reduce #(if-let [errors (check-rule %2 path data)]
                (concat %1 errors)
                %1)
             '() rules))))

(defn nested-coll
  [path rules]
  (fn [data]
    (apply concat (map #(let [validator (validate rules (conj path %2))]
                          (validator %1))
                       (get-in data path) (range)))))


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
