(ns
    #^{:author "Paavo Parkkinen"
       :doc "Functions for working with mesh graphs"}
  pparkkin.mesh-graph
  (:use (pparkkin weighted-graph point)))

;;; Stuff you can try:
;;; (def g (ref (struct weighted-directed-graph #{} {})))
;;; (open-frame g)
;;; (dosync (alter g random-fill 30 (frame-width) (frame-height)))
;;; (dosync (alter g connect-neighbours
;;;                  (partial > 10000)
;;;                  (fn [p1 p2] (math/expt (distance p1 p2) 2))))

(defn random-point
  "Return a random point"
  [width height]
  (struct point
          (rand-int width)
          (rand-int height)))

(defn random-fill
  "Insert n random points into a graph"
  [g n width height]
  (add-nodes g n (fn [_] (random-point width height))))

(defn connect-neighbours
  "Connect near-by points with edges"
  ([g l]
     (connect-neighbours
      g
      (fn [d]
        (and (< d l)
             (not= d 0)))
      distance))
  ([g p f]
     (add-edges g
                (fn [p1 p2]
                  (let [distance (f p1 p2)]
                    (when (p distance) distance))))))
