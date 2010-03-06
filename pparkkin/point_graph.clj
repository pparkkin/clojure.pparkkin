(ns
    #^{:author "Paavo Parkkinen"
       :doc "Functions for working with graphs of points"}
  pparkkin.point-graph
  (:use (pparkkin weighted-graph point)))

(defn random-point
  "Return a random point in the rectangle (0,0) to (width,height)"
  [width height]
  (struct point
          (rand-int width)
          (rand-int height)))

(defn random-fill
  "Insert n random points into a graph
  The points will be inside the rectangle (0,0) to (width,height)"
  [g n width height]
  (add-nodes g n (fn [_] (random-point width height))))

(defn connect-neighbors
  "Connect near-by points with edges
  Parameters can be either a single number l, or a distance
  function f and a predicate p.
  If a single l is given, all the neighbors closer to l to a point
  will be connected to that point.
  If an f and a p are given, f will be used to determine the distance
  between two points, and if p, given that distance, returns true, the
  two points will be connected.
  The weights of connecting edges will be the distance between the
  connected points."
  ([g l]
     (connect-neighbors
      g
      (fn [d]
        (and (< d l)
             (not= d 0)))
      distance))
  ([g p f]
     (add-edges g
                (fn [p1 p2]
                  (let [l (f p1 p2)]
                    (when (p l) l))))))
