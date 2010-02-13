(ns
    #^{:author "Paavo Parkkinen"
       :doc "A point on a 2-dimensional plane"}
  pparkkin.point
  (:require (clojure.contrib [math :as math])))

(defstruct point
  :x
  :y)

(defn distance
  "Calculate distance between two points"
  [p1 p2]
  (let [x1 (:x p1)
        x2 (:x p2)
        y1 (:y p1)
        y2 (:y p2)]
    (math/sqrt (+ (math/expt (- x2 x1) 2)
                  (math/expt (- y2 y1) 2)))))
