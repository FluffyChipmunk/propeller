(ns propeller.push.instructions.numeric
  "FLOAT and INTEGER Instructions (polymorphic).
  Additional instructions can be found at [Additional Instructions](Additional_Instructions.md)."
  (:require [propeller.tools.math :as math]
            [propeller.push.instructions :refer [def-instruction
                                                 generate-instructions
                                                 make-instruction]]))

;; =============================================================================
;; FLOAT and INTEGER Instructions (polymorphic)
;; =============================================================================

;; Pushes TRUE onto the BOOLEAN stack if the first item is greater than the second
;; item, and FALSE otherwise
(def _gt
  "Pushes TRUE onto the BOOLEAN stack if the first item is greater
  than the second item, and FALSE otherwise"
  ^{:stacks #{:boolean}
    :name "_gt"}
  (fn [stack state]
    (make-instruction state > [stack stack] :boolean)))

;; Pushes TRUE onto the BOOLEAN stack if the second item is greater than or
;; equal to the top item, and FALSE otherwise
(def _gte
  "Pushes TRUE onto the BOOLEAN stack if the second item is greater than or
  equal to the top item, and FALSE otherwise"
  ^{:stacks #{:boolean}
    :name "_gte"}
  (fn [stack state]
    (make-instruction state >= [stack stack] :boolean)))

;; Pushes TRUE onto the BOOLEAN stack if the second item is less than the top
;; item, and FALSE otherwise
(def _lt
  "Pushes TRUE onto the BOOLEAN stack if the second item is less than the top
  item, and FALSE otherwise"
  ^{:stacks #{:boolean}
    :name "_lt"}
  (fn [stack state]
    (make-instruction state < [stack stack] :boolean)))

;; Pushes TRUE onto the BOOLEAN stack if the second item is less than or equal
;; to the top item, and FALSE otherwise
(def _lte
  "Pushes TRUE onto the BOOLEAN stack if the second item is less than or equal
  to the top item, and FALSE otherwise"
  ^{:stacks #{:boolean}
    :name "_lte"}
  (fn [stack state]
    (make-instruction state <= [stack stack] :boolean)))

;; Pushes the sum of the top two items onto the same stack
(def _add
  "Pushes the sum of the top two items onto the same stack"
  ^{:stacks #{}
    :name "_add"}
  (fn [stack state]
    #?(:clj (make-instruction state +' [stack stack] stack)
       :cljs (make-instruction state + [stack stack] stack))))

;; Pushes the difference of the top two items (i.e. the second item minus the
;; top item) onto the same stack
(def _subtract
  "Pushes the difference of the top two items (i.e. the second item minus the
  top item) onto the same stack"
  ^{:stacks #{}
    :name "_subtract"}
  (fn [stack state]
    #?(:clj (make-instruction state -' [stack stack] stack)
       :cljs (make-instruction state - [stack stack] stack))))

;; Pushes the product of the top two items onto the same stack
(def _mult
  "Pushes the product of the top two items onto the same stack"
  ^{:stacks #{}
    :name "_mult"}
  (fn [stack state]
    #?(:clj (make-instruction state *' [stack stack] stack)
       :cljs (make-instruction state * [stack stack] stack))))

;; Pushes the quotient of the top two items (i.e. the first item divided by the
;; second item) onto the same stack. If the second item is zero, pushes 1
(def _quot
  "Pushes the quotient of the top two items (i.e. the first item divided by the
  second item) onto the same stack. If the second item is zero, pushes 1"
  ^{:stacks #{}
    :name "_quot"}
  (fn [stack state]
    (make-instruction state #(if (zero? %2) 1 (quot %1 %2)) [stack stack] stack)))

;; Pushes the top item modulo the second item onto the same stack. If the second
;; item is zero, pushes 1. The modulus is computed as the remainder of the
;; quotient, where the quotient has first been truncated towards negative
;; infinity.
(def _mod
  "Pushes the top item modulo the second item onto the same stack. If the second
  item is zero, pushes 1. The modulus is computed as the remainder of the
   quotient, where the quotient has first been truncated towards negative
   infinity."
  ^{:stacks #{}
    :name "_mod"}
  (fn [stack state]
    (make-instruction state #(if (zero? %2) 1 (mod %1 %2)) [stack stack] stack)))

;; Pushes the maximum of the top two items
(def _max
  "Pushes the maximum of the top two items"
  ^{:stacks #{}
    :name "_max"}
  (fn [stack state]
    (make-instruction state max [stack stack] stack)))

;; Pushes the minimum of the top two items
(def _min
  "Pushes the minimum of the top two items"
  ^{:stacks #{}
    :name "_min"}
  (fn [stack state]
    (make-instruction state min [stack stack] stack)))

;; Pushes 1 / 1.0 if the top BOOLEAN is TRUE, or 0 / 0.0 if FALSE
(def _from_boolean
  "Pushes 1 / 1.0 if the top BOOLEAN is TRUE, or 0 / 0.0 if FALSE"
  ^{:stacks #{:boolean}
    :name "_from_boolean"}
  (fn [stack state]
    (make-instruction state
                      #((if (= stack :integer) int float) (if % 1 0))
                      [:boolean]
                      stack)))

;; Pushes the ASCII value of the top CHAR
(def _from_char
  "Pushes the ASCII value of the top CHAR"
  ^{:stacks #{:char}
    :name "_from_char"}
  (fn [stack state]
    (make-instruction state (if (= stack :integer) int float) [:char] stack)))

;; Pushes the value of the top STRING, if it can be parsed as a number.
;; Otherwise, acts as a NOOP
(def _from_string
  "Pushes the value of the top STRING, if it can be parsed as a number.
Otherwise, acts as a NOOP"
  ^{:stacks #{:string}
    :name "_from_string"}
  (fn [stack state]
    (make-instruction state
                      #(try (if (= stack :integer)
                             #?(:clj (Integer/parseInt %)
                                :cljs (js/parseInt %)) 
                             #?(:clj (Float/parseFloat %) 
                                :cljs (js/parseFloat %)))
                            #?(:clj (catch Exception e :ignore-instruction)
                               :cljs (catch js/Error e :ignore-instruction)))
                      [:string]
                      stack)))

;; Pushes the increment (i.e. +1) of the top item of the stack
(def _inc
  "Pushes the increment (i.e. +1) of the top item of the stack"
  ^{:stacks #{}
    :name "_inc"}
  (fn [stack state]
    (make-instruction state inc [stack] stack)))

;; Pushes the decrement (i.e. -1) of the top item of the stack
(def _dec
  "Pushes the decrement (i.e. -1) of the top item of the stack"
  ^{:stacks #{}
    :name "_dec"}
  (fn [stack state]
    (make-instruction state dec [stack] stack)))

;; 2 types x 16 functions = 32 instructions
(generate-instructions
  [:float :integer]
  [_gt _gte _lt _lte _add _subtract _mult _quot _mod _max _min _inc _dec
   _from_boolean _from_char _from_string])

;; =============================================================================
;; FLOAT Instructions only
;; =============================================================================

;; Pushes the cosine of the top FLOAT
(def-instruction
  :float_cos
  ^{:stacks #{:float}}
  (fn [state]
    (make-instruction state math/cos [:float] :float)))

;; Pushes the sine of the top FLOAT
(def-instruction
  :float_sin
  ^{:stacks #{:float}}
  (fn [state]
    (make-instruction state math/sin [:float] :float)))

;; Pushes the tangent of the top FLOAT
(def-instruction
  :float_tan
  ^{:stacks #{:float}}
  (fn [state]
    (make-instruction state math/tan [:float] :float)))

;; Pushes the floating point version of the top INTEGER
(def-instruction
  :float_from_integer
  ^{:stacks #{:float :integer}}
  (fn [state]
    (make-instruction state float [:integer] :float)))

;; =============================================================================
;; INTEGER Instructions only
;; =============================================================================

;; Pushes the result of truncating the top FLOAT towards negative infinity
(def-instruction
  :integer_from_float
  ^{:stacks #{:float :integer}}
  (fn [state]
    (make-instruction state int [:float] :integer)))
