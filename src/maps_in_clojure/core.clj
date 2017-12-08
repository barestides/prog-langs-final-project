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

;; With all data types in clojure being immutable by default, there needs to be a way to
;; handle persistent state.
;; Clojure does this using atoms, refs and agents. We will only be talking about atoms here.

;; To create an atom, the `atom` function is used on some value
(def number-atom (atom 1))

;; To access the value of the atom, it must be dereferenced with `deref`, or @ as a shortcut

number-atom
;; => #atom[1 0x1acc3266]

@number-atom
;; => 1

;; To enforce general immutability, clojure makes updating an atom very explicit.
;; By convention, any function that changes persistent state should end in an exclamtion mark

;;Let's make a function to increment the number-atom:
(defn inc-atom [atom]
  (swap! atom inc))

(inc-atom number-atom)
;; => 2
@number-atom
;; => 2

;; the `swap!` function is used to change an atom's value by applying a function to it.
;;There also exists `reset!`, which is for resetting an atom's value to a given value
;;These are analogous to `update` and `assoc` when working with maps.

(reset! number-atom 1)
@number-atom
;;now the value is back to 1
;; => 1

;;Putting it all together

;;Now with our knowledge of working with nested maps, along with our understanding of atoms,
;;we can create a simple backend for a webapp that displays people's pets

;;We need to have some sort of global state map to keep track of everyone's pets:

(def state (atom {}))

;; we'll initialize empty and add to it, first we need some functions that represent actions
;; on the site

(defn create-account! [name age]
  (swap! state assoc name {:age age}))

(defn delete-account! [name]
  (swap! state dissoc name))

(defn add-pet! [name animal pet-info]
  (swap! state assoc-in [name :pets animal] pet-info))

;;@state
;; => {}

(create-account! :braden 22)

;;@state
;; => {:braden {:age 22}}
(add-pet! :braden :bird {:name "Dante" :age 13 :color "red"})

;;@state
;; => {:braden {:pets {:bird {:name "Dante", :age 13, :color "red"}}}}

(delete-account! :braden)
;;@state
;; => {}
