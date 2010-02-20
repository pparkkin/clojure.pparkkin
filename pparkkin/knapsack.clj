(ns
    #^{:author "Paavo Parkkinen"
       :doc "Version of the knapsack algorithm"}
  pparkkin.knapsack)

(defn sums
  "Return a set of all possible sums from a sequence of positive integers"
  [s]
  (loop [i s
         result #{0}]
    (if (empty? i)
      result
      (recur (rest i)
             (reduce (fn [s n]
                       (clojure.set/union
                        (set (list (+ n (first i)) (first i) n))
                        s))
                     #{}
                     result)))))

(defn have-same?
  "Do the two sets have the same element?
  Only works for stuff that compare works on."
  [s1 s2]
  (not (nil?
        (loop [ss1 (apply sorted-set s1)
               ss2 (apply sorted-set s2)]
          (let [f1 (first ss1)
                f2 (first ss2)
                d (compare (first ss1) (first ss2))]
            (cond
              (or (nil? f1) (nil? f2))
              nil
              (= d 0)
              f1
              (< d 0)
              (recur (rest ss1) ss2)
              (> d 0)
              (recur ss1 (rest ss2))))))))

(defn knapsack1974
  "The knapsack algorithm from 1974"
  [s b]
  (let [[h t] (split-at (/ (count s) 2) s)]
    (have-same?
     (sums h)
     (set (map (fn [n]
                 (- b n))
               (sums t))))))

(defn knapsack
  "A knapsack algorithm"
  [s b]
  (knapsack s b))
