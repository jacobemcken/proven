(ns proven.validator
  (:require [clojure.string :as string]))

(defn get-field-name
  [key-translation k]
  (get key-translation k (name k)))

(defn human-concat
  "Concat items (which should be a collection of strings) in a human readable
manner. Ie.:
'(\"item1\")                               => \"item1\"
'(\"item1\" \"item2\")                     => \"item1 & item2\"
'(\"item1\" \"item2\" \"item3\")           => \"item1, item2 & item3\"
'(\"item1\" \"item2\" \"item3\" \"item4\") => \"item1, item2, item3 & item4\""
  [items]
  (->>
   (list (string/join ", " (butlast items)) (last items))
   (remove empty?)
   (string/join " & ")))

(defn error-fields
  "Generates a string (human readable) of the concatination of all leaves in paths."
  [paths key-translation]
  (let [translate #(get-field-name key-translation (last %))]
    (human-concat (map translate paths))))

(defn error-strings
  "Takes a list of errors and an optional translation map.
The translation map is used to look up human readable versions of the path leafs
(usually a keyword matching the form input field). Returns a list of strings
describing the errors"
  ([errors]
   (error-strings {}))
  ([errors translations]
   (map (fn [err] (str (error-fields (:paths err) translations) " " (:msg err))) errors)))

(defn apply-path
  "Takes path prefix (a vector) and a set of (relative) paths and returns a new
set of (absolute) paths, where the prefix has been applied.

> (apply-path [:some-prefix] #{[:key1] [:key2 :key3]}))
#{[:some-prefix :key1] [:some-prefix :key2 :key3]}"
  [path-prefix relative-paths]
  (->>
   (map #(into [] (concat path-prefix %)) relative-paths)
   (into (hash-set))))

(defn apply-rule
  [path-prefix data rule]
  (when-let [errors (not-empty (rule data))]
    (map #(update % :paths (partial apply-path path-prefix)) errors)))

; Validation functions

(defn validate
  ([rules data]
   (validate rules data []))
  ([rules data path-prefix]
   (apply concat (keep #(apply-rule path-prefix data %) rules))))

(defn for-all
  [path rules]
  (fn [data]
    (apply concat (map #(validate rules %1 (conj path %2))
                       (get-in data path)
                       (range) ; range calculates the index used for the collection in path
                       ))))

(defn apply-in
  "Apply specific rules at nested map"
  [path rules]
  (fn [data-parent]
    (validate rules (get-in data-parent path) path)))

(defn upon
  ([check rules1]
   (upon check rules1 []))
  ([check rules1 rules2]
   (fn [data]
     (validate (if (check data) rules1 rules2) data))))
