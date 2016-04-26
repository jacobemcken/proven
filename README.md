# Proven



A Clojure validation library **with emphasis on human readable error messages**
that goes beyond "just a type". As opposed to the validation provided by
libraries like [Schema][] and [core.typed][] it validates an entire map as a
whole. This allows for differentiating validation of one valued based on the
value of another. Inspiration for this library came from [jkk/verily][verily]
but this lib encourage nested data structures.

The intended use case is to validate a data structure behind a form in ie. when
using [Om][] or something like it.

Examples of not "just a type" validation:

  * That two email adresses in a map is the same.
  * Only validate fields if other field(s) has been filled in.
    Assume entering a credit card is optional but when specifying the card
    number both expiry date and control number becomes required.

The error (message) is central in `proven` validation, and a validation error
can reference one or multiple keys (paths) in the map that caused the validation
error. This also means that a single key in a map can have multiple different
errors attached to it. The examples below will show this.

Proven returns human readable errors that can be shipped back to end users!

[Schema]: https://github.com/Prismatic/schema
[core.typed]: https://github.com/clojure/core.typed
[Om]: https://github.com/omcljs/om


## Installation

Add the following dependency to your `project.clj` file:

    [proven "0.1.1"]

[![Clojars Project](https://img.shields.io/clojars/v/proven.svg)](https://clojars.org/proven)


## Usage

The validation function takes a collection of rules and the hash-map to be
validated. A rule is a function that takes a hash-map and returns an empty list
when data within it is valid and list of validation errors when not.

The namespace `proven.rule` contains helper functions to build rules for most
common cases. But see "Built in validations" further down for a complete list.


### The basics

A very basic example to get started:

```clj
(ns example.core
  (:require [proven.rule :as rule]
            [proven.validator :refer [validate]]))

(def rules
  [(rule/required :last-name)
   (rule/not-blank :first-name)])

(validate rules {:last-name "Smith"})

;; Return an empty list which means there is no validation errors
()


(validate rules {:first-name ""})

;; Return a list with Err records describing all validation errors
(#proven.core.Err{:paths #{[:last-name]},
                  :msg "must not be blank or missing"}
 #proven.core.Err{:paths #{[:first-name]},
                  :msg "must not be blank"})
```

The list of errors is the biggest difference from [jkk/verily][verily] because
instead of referencing keys the `Err` record references a path (in a nested
structure).


### Conditionals

Now for some conditional rules:

```clj
(ns example.core
  (:require [proven.rule :as rule]
            [proven.validator :refer [validate upon]]))

(def private-rules
  [(rule/required :last-name)
   (rule/not-blank :first-name)])

(def company-rules
  [(rule/required [:company-name])])

(def rules
  [(upon #(= (:type %) "private")
         private-rules
         company-rules)])

(validate rules {:type "private" :last-name "Smith"})

;; No validation errors
()

(validate rules {:type "company" :last-name "Smith"})

;; When type is "company" (or at least not "private")
;; Then `:company-name` is a reuqired key
(#proven.core.Err{:paths #{[:company-name]},
                  :msg "must not be blank or missing"})
```


### Nested rules

```clj
(ns example.core
  (:require [proven.rule :as rule]
            [proven.validator :refer [validate in-coll]]))

(def person-rules
  [(rule/required :last-name)])

(def rules
  [(rule/min-length 1 :persons)
   (in-coll [:persons] person-rules)])

(def data
  {:persons [{:last-name "Smith"}
             {:first-name "John"}
             {:last-name "Doe"}]})

(validate rules data)

;; The path can now be used to reference the exact value The validation path now points exactly
(#proven.core.Err{:paths #{[:persons 1 :last-name]},
                  :msg "must not be blank or missing"})

(def corrected-data
  (assoc-in data [:persons 1 :last-name] "Smith"))

(validate rules corrected-data)
;; No validation errors
()
```

When representing a nested data structure in a graphical user interface
ie. a web form, the path can be used to highlight the form and/or specific form
fields which has led to the validation error.


### Built-in rules

The built-in rules aren't rules pr. say but rather rule builders (functions that
return the actual validation function). This fact might become more clear when
looking at "Custom rules" below. For now just know that all `proven.rule` buildes
takes the key (or keys) and an optional error message as the last arguments.

  * `required` - key(s) must be present in the map and be not blank (blank being nil or empty string)
  * `not-blank` - key(s) if is present they must be not blank
  * `contains` - key(s) must be present but can have whatever value (even nil)
  * `exact value` - key(s) if present must contain excatly the specified value
  * `matches re-pattern` - key(s) if present must match the regular expression or be blank
  * `min-length length` - key(s) if present must have a value with the specified minimum length (can be both strings and collections - everything that `count` works on)
  * `max-length length` - kinda like `min-length` except it isn't :-P
  * `exact-length length` - kinda like `min-length` except it isn't :-P
  * `between-length [lower upper]` - a convinience to avoid having to make both a `min-length` and `max-length` in some situations

Some examples usages of rule building:

```clj
(ns example.core
  (:require [proven.rule :as rule]))

(def rules
  [(rule/required :last-name) ; apply rule to single key
   (rule/not-blank [:first-name :middle-name])  ; apply rule to multiple keys
   (rule/min-length 6 :password) ; rule builder with an argument other than key(s)
   (rule/max-length 160 :tweet "Tweets cannot exceed 160 characters") ; rule builder with an optional error message
   (rule/exact "secretcode" [:code :repeat-code] "must match \"secretcode\"")
   (matches #"[0-9]{5}" :zip "must contain excatly 5 digits")
   ])
```

### Custom rules

A Proven "rule" is a function that takes a map and returns a list of
errors if the validation fails. Upon valid input the rule function
will return an empty list or nil indicating that there is no errors.
An error is specified using the record `#proven.core.Err`.


#### Low-level

How to build a rule without the `proven.rule` namespace which contains
helper functions:

```clj
(ns example.core
  (:require [proven.validator :refer [validate]]
            #?(:cljs [proven.core :refer [Err]]))
  #?(:clj (:import [proven.core Err])))

(defn name-is-john
  [m]
  (when-not (= (:name m) "John")
    (list (Err. #{[:name]} "must be \"John\""))))

(validate [name-is-john] {:name "John"})

;; Return an empty list which means there is no validation errors
()

(validate [name-is-john] {:name "James"})

;; Return error list
(#proven.core.Err{:paths #{[:name]}, :msg "must be \"John\""})
```

#### Rules on a higher level

By using some of the building blocks of the Proven it is easier to
make reusable rules. The function `make-validator` returns a Proven
rule (a function) which is a convenience for easily applying the same
rule to multiple keys in a map and also makes it easier to overwrite
the error message (i.e. for translation).

```clj
(ns example.core
  (:require [proven.rule :as rule]
            [proven.validator :refer [validate]]))

(defn equals-john
  [ks & [msg]]
  (rule/make-validator
   ks
   #(not= % "John")
   (or msg "must be \"John\"")))

(validate [(equals-john :alias "Gimme \"John\"")] {:alias "John"})

;; Return an empty list which means there is no validation errors
()

(validate
 [(equals-john [:name :alias] "Gimme \"John\"")]
 {:name "John" :alias "James"})

;; Return error list
(#proven.core.Err{:paths #{[:alias]}, :msg "Gimme \"John\""})
```

Also sometimes you only want to apply a rule if the key in the map
actually exists:

```clj
(ns example.core
  (:require [proven.rule :as rule]
            [proven.validator :refer [validate]]))

(defn equals-john
  [ks & [msg]]
  (rule/make-validator
   ks
   (rule/when-present #(= % "John"))
   (or msg "must be \"John\"")))

(validate
 [(equals-john [:name :alias])]
 {:alias "John"})

;; Notice how the omitted :name doesn't bother the validation rule
()
```


## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


[verily]:https://github.com/jkk/verily