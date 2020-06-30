(ns propeller.push.instructions.polymorphic
  #?(:cljs (:require-macros [propeller.push.utils :refer [generate-instructions
                                                          make-instruction]]))
  (:require [propeller.utils :as utils]
            [propeller.push.state :as state]
            #?(:clj [propeller.push.utils :refer [generate-instructions
                                                  make-instruction]])))

;; =============================================================================
;; Polymorphic Instructions
;;
;; (for all stacks, with the exception of non-data ones like auxiliary, input,
;; and output)
;; =============================================================================

;; Duplicates the top item of the stack. Does not pop its argument (since that
;; would negate the effect of the duplication)
(def _dup
  ^{:stacks #{}}
  (fn [stack state]
    (let [top-item (state/peek-stack state stack)]
      (if (state/empty-stack? state stack)
        state
        (state/push-to-stack state stack top-item)))))

;; Duplicates n copies of the top item (i.e leaves n copies there). Does not pop
;; its argument (since that would negate the effect of the duplication). The
;; number n is determined by the top INTEGER. For n = 0, equivalent to POP.
;; For n = 1, equivalent to NOOP. For n = 2, equivalent to DUP. Negative values
;; of n are treated as 0
(def _duptimes
  ^{:stacks #{:integer}}
  (fn [stack state]
    (if (or (and (= stack :integer)
                 (<= 2 (count (:integer state))))
            (and (not= stack :integer)
                 (not (state/empty-stack? state :integer))
                 (not (state/empty-stack? state stack))))
      (let [n (state/peek-stack state :integer)
            popped-state (state/pop-stack state :integer)
            top-item (state/peek-stack popped-state stack)
            top-item-dup (take (- n 1) (repeat top-item))]
        (cond
          (< 0 n) (state/push-to-stack-multiple popped-state stack top-item-dup)
          :else (state/pop-stack popped-state stack)))
      state)))

;; Duplicates the top n items on the stack, one time each. The number n is
;; determined by the top INTEGER. If n <= 0, no items will be duplicated. If
;; fewer than n items are on the stack, the entire stack will be duplicated.
(def _dupitems
  ^{:stacks #{:integer}}
  (fn [stack state]
    (if (state/empty-stack? state :integer)
      state
      (let [n (state/peek-stack state :integer)
            popped-state (state/pop-stack state :integer)
            top-items (take n (get popped-state stack))]
        (state/push-to-stack-multiple popped-state stack top-items)))))

;; Pushes TRUE onto the BOOLEAN stack if the stack is empty. Otherwise FALSE
(def _empty
  ^{:stacks #{:boolean}}
  (fn [stack state]
    (state/push-to-stack state :boolean (state/empty-stack? state stack))))

;; Pushes TRUE onto the BOOLEAN stack if the top two items are equal.
;; Otherwise FALSE
(def _eq
  ^{:stacks #{:boolean}}
  (fn [stack state]
    (make-instruction state = [stack stack] :boolean)))

;; Empties the given stack
(def _flush
  ^{:stacks #{}}
  (fn [stack state]
    (assoc state stack '())))

;; Pops the given stack
(def _pop
  ^{:stacks #{}}
  (fn [stack state]
    (state/pop-stack state stack)))

;; Rotates the top three items on the stack (i.e. pulls the third item out and
;; pushes it on top). Equivalent to (yank state stack-type 2)
(def _rot
  ^{:stacks #{}}
  (fn [stack state]
    (if (<= 3 (count (get state stack)))
      (let [top-three (state/peek-stack-multiple state stack 3)
            popped-state (state/pop-stack-multiple state stack 3)
            top-three-rot (take 3 (conj top-three (last top-three)))]
        (state/push-to-stack-multiple popped-state stack top-three-rot))
      state)))

;; Inserts the top item deeper into the stack, using the top INTEGER to
;; determine how deep
(def _shove
  ^{:stacks #{:integer}}
  (fn [stack state]
    (if (or (and (= stack :integer)
                 (<= 2 (count (:integer state))))
            (and (not= stack :integer)
                 (not (state/empty-stack? state :integer))
                 (not (state/empty-stack? state stack))))
      (let [index-raw (state/peek-stack state :integer)
            popped-state (state/pop-stack state :integer)
            top-item (state/peek-stack popped-state stack)
            popped-state (state/pop-stack popped-state stack)
            index (max 0 (min index-raw (count (get popped-state stack))))]
        (update popped-state stack #(utils/not-lazy (concat (take index %)
                                                            (list top-item)
                                                            (drop index %)))))
      state)))

;; Pushes the given stack's depth onto the INTEGER stack
(def _stackdepth
  ^{:stacks #{:integer}}
  (fn [stack state]
    (let [stack-depth (count (get state stack))]
      (state/push-to-stack state :integer stack-depth))))

;; Swaps the top two items on the stack
(def _swap
  ^{:stacks #{}}
  (fn [stack state]
    (if (<= 2 (count (get state stack)))
      (let [top-two (state/peek-stack-multiple state stack 2)
            popped-state (state/pop-stack-multiple state stack 2)]
        (state/push-to-stack-multiple popped-state stack (reverse top-two)))
      state)))

;; Pushes an indexed item from deep in the stack, removing it. The top INTEGER
;; is used to determine how deep to yank from
(def _yank
  ^{:stacks #{:integer}}
  (fn [stack state]
    (if (or (and (= stack :integer)
                 (<= 2 (count (:integer state))))
            (and (not= stack :integer)
                 (not (state/empty-stack? state :integer))
                 (not (state/empty-stack? state stack))))
      (let [index-raw (state/peek-stack state :integer)
            popped-state (state/pop-stack state :integer)
            index (max 0 (min index-raw (count (get popped-state stack))))
            indexed-item (nth (get popped-state stack) index)]
        (update popped-state stack #(utils/not-lazy
                                      (concat (list indexed-item)
                                              (take index %)
                                              (rest (drop index %))))))
      state)))

;; Pushes a copy of an indexed item from deep in the stack, without removing it.
;; The top INTEGER is used to determine how deep to yankdup from
(def _yankdup
  ^{:stacks #{:integer}}
  (fn [stack state]
    (if (or (and (= stack :integer)
                 (<= 2 (count (:integer state))))
            (and (not= stack :integer)
                 (not (state/empty-stack? state :integer))
                 (not (state/empty-stack? state stack))))
      (let [index-raw (state/peek-stack state :integer)
            popped-state (state/pop-stack state :integer)
            index (max 0 (min index-raw (count (get popped-state stack))))
            indexed-item (nth (get popped-state stack) index)]
        (state/push-to-stack popped-state stack indexed-item))
      state)))

;; 9 types x 1 functions = 9 instructions
(generate-instructions
  [:boolean :char :float :integer :string
   :vector_boolean :vector_float :vector_integer :vector_string]
  [_eq])

;; 11 types x 12 functions = 132 instructions
(generate-instructions
  [:boolean :char :code :exec :float :integer :string
   :vector_boolean :vector_float :vector_integer :vector_string]
  [_dup _duptimes _dupitems _empty _flush _pop _rot _shove _stackdepth
   _swap _yank _yankdup])