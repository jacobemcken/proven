# Proven

Validation library inspired by [jkk/verily][verily] but with better support for
nested structures.


## Installation

Add the following dependency to your project.clj file:

    [proven "0.1.0"]

[![Clojars Project](https://img.shields.io/clojars/v/proven.svg)](https://clojars.org/proven)


## Usage

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
  [(in-coll [:persons] person-rules)])

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


## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


[verily]:https://github.com/jkk/verily