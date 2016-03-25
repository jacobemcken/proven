(ns proven.core
  (:require [clojure.string :as string]))

;; A paths nesting is described by vector of one or more keys
;; Paths is a set containing
(defrecord Err [paths msg])

(defn seqify [x]
  (if-not (or (sequential? x) (set? x)) [x] x))
