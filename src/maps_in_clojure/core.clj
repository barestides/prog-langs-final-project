(ns maps-in-clojure.core
  (:gen-class))

;; Clojure is a lisp dialect functional language that runs on top of the JVM
;; JVM handles the memory management similarly to how it does with Java.
;; It also provides Clojure with Java interop, allowing it to use Java methods in Clojure code

;; This writeup will go over working with deeply nested maps in clojure and will introduce working
;; with persistent state.

;; Without object orientation, deeply nested maps are a very popular data structure.
;; These can quickly become complicated. Fortunately, clojure provides several native functions
;; for working with them.

;; We will work with this example map of people and their pets to explain these features
(def people {:braden {:name "Braden"
                      :age 22
                      :pets {:cats {:dana {:age 3
                                           :color "black"}
                                    :luke {:age 1
                                           :color "grey"}}
                             :fish {:jorge {:species "molly"
                                            :color "white"}
                                    :greg {:species "Tetra"
                                           :color "blue"}}}}
             :anne {:name "Anne"
                    :age 54
                    :pets {:birds {:allie {:age 11
                                           :color "grey"}}}}})

;; This map is 5 levels at its deepest level. In a language such as Java, this would be unnacceptably
;; complex, and would be better dealt with using object orientation, but clojure provides the necessary
;; features for this kind of map to be easily worked with.

;; `get`, `assoc`, `dissoc`, and `update`

;; `get` is used to pluck a value out of a map:
(get people :braden)

;;However, it is more idiomatic to use the keyword as a function for pulling values out of a map:
(:braden people)

;;The above two functions do the same thing. The `get` function should be used when the keys of a map are;; strings rather than keywords.

;;let's create a new map from the larger map
(def braden (:braden people))

;; `:assoc` is used for setting the value for a key in a map. This can be a new key or an existing one:
;; any number of keys can be set in the assoc function call
(assoc braden :age 25 :name "Braden Arestides")

;; =>
;; {:name "Braden Arestides",
;;  :age 25,
;;  :pets
;;  {:cats {:dana {:age 3, :color "black"}, :luke {:age 1, :color "grey"}},
;;   :fish {:jorge {:species "molly", :color "white"}, :greg {:species "Tetra", :color "blue"}}}}

;; Note that like haskell, this does not update the original data structure, but creates a new one.
;; I'll describe how clojure handles persistent state later

;; `dissoc` is used to remove keys from a map, effectively setting them to `nil`
(dissoc braden :pets :age)

;; =>
;; {:name "Braden" :age 22}

;; `update` is similar to `assoc`, except that a function is passed rather than a value.
;; This function is passed the value for the key, and the value returned from the function is placed
;; into the map

(update braden :age inc)
;; =>
;; {:name "Braden",
;;  :age 23,
;;  :pets
;;  {:cats {:dana {:age 3, :color "black"}, :luke {:age 1, :color "grey"}},
;;   :fish {:jorge {:species "molly", :color "white"}, :greg {:species "Tetra", :color "blue"}}}}

;; Here, the key for the map is `:age`, we pluck out the value for `:age`, and pass it to the supplied
;; function, `inc` in this case. `inc` returns the value plus one, and then update replaces the original
;; value for the key

(assoc braden :age (inc (get braden :age)))
;;the above statement does the same thing as the update statement

;; Ok, but what about nested maps?
;; There also exist `get-in`, `assoc-in`, and `update-in` for nested maps.
;; Instead of taking a single key, these functions take a sequence of keys that delve into a nested map

(get-in people [:braden :pets :fish :jorge :species])
;; => molly

(assoc-in people [:anne :pets :birds :allie :color] "red")

;; =>
;; {:braden
;;  {:name "Braden",
;;   :age 22,
;;   :pets
;;   {:cats {:dana {:age 3, :color "black"}, :luke {:age 1, :color "grey"}},
;;    :fish {:jorge {:species "molly", :color "white"}, :greg {:species "Tetra", :color "blue"}}}},
;;  :anne {:name "Anne", :age 54, :pets {:birds {:allie {:age 11, :color "`red`"}}}}}

(update-in people [:braden :pets :cats :luke :age] inc)

;; =>
;; {:braden
;;  {:name "Braden",
;;   :age 22,
;;   :pets
;;   {:cats {:dana {:age 3, :color "black"}, :luke {:age `2`, :color "grey"}},
;;    :fish {:jorge {:species "molly", :color "white"}, :greg {:species "Tetra", :color "blue"}}}},
;;  :anne {:name "Anne", :age 54, :pets {:birds {:allie {:age 11, :color "grey"}}}}}

;; Why no `dissoc-in`?
;; dissocing in a nested map can be done by using an `update-in` with `dissoc` as the function arglists

(update-in people [:braden :pets :fish] dissoc :greg)

;; =>
;; {:braden
;;  {:name "Braden",
;;   :age 22,
;;   :pets
;;   {:cats {:dana {:age 3, :color "black"}, :luke {:age 1, :color "grey"}},
;;    :fish {:jorge {:species "molly", :color "white"}}}},
;;  :anne {:name "Anne", :age 54, :pets {:birds {:allie {:age 11, :color "grey"}}}}}
