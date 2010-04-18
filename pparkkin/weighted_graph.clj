(ns
    #^{:author "Paavo Parkkinen"
       :doc "An implementation of a weighted, directed graph
             using incidence lists"}
  pparkkin.weighted-graph)

(defstruct weighted-directed-graph ; implemented using incidence lists
  :nodes       ; set of nodes (#{})
  :incidences) ; a map from nodes to incidences ({})

(defstruct incidence
  :to      ; node the incidence is pointing to
  :weight) ; weight of edge

(defn get-nodes
  "Get the nodes of a graph"
  [g]
  (:nodes g))

(defn add-node
  "Add a node to a graph"
  [g n]
  (assoc g :nodes (conj (get-nodes g) n)))

(defn add-nodes
  "Add a collection of nodes to a graph
  Provided as either a collection of nodes or as an amount and a
  function that will provide the nodes."
  ([g ns]
     (assoc g :nodes (clojure.set/union (get-nodes g) (set ns))))
  ([g n f]
     (add-nodes g (map f (range n)))))

;; This doesn't work: The incidences (edges) connected to the node
;; aren't removed
;; TODO: Fix
;(defn remove-node
;  "Remove a node from a graph"
;  ([g n]
;     (assoc g :nodes (filter (fn [k]
;                               (not= k n))
;                             (get-nodes g)))))

(defn clear-nodes
  "Clear all nodes"
  [g]
  (struct weighted-directed-graph #{} {}))

(defn map-nodes
  "Map f to all nodes of g"
  [f g]
  (assoc g :nodes
         (into #{}
               (map f (:nodes g)))))

(defn get-incidences
  "Get the incidences of a node"
  [g n]
  (if (:incidences g)
    ((:incidences g) n)
    #{}))

(defn add-edge
  "Add an edge from a node to another node with a weight"
  [g from to weight]
  (assoc g
    :incidences (into {}
                      (map (fn [n]
                             [n (if (= n from)
                                  (conj (set (get-incidences g n))
                                        (struct incidence to weight))
                                  (set (get-incidences g n)))])
                           (get-nodes g)))))

(defn add-edges
  "Add a number of edges at once
  For all node pairs the function f is called and it should return the
  weight of the edge between them, or nil if there should not be an
  edge between them."
  [g f]
  (assoc g
    :incidences (into {}
                      (map (fn [n]
                             [n (clojure.set/union
                                 (set (get-incidences g n))
                                 ;; the following could probably be
                                 ;; replaced by a reduce
                                 (into #{}
                                       (filter (fn [x]
                                                 (not (nil? x)))
                                               (map (fn [k]
                                                      (let [w (f n k)]
                                                        (when w
                                                          (struct incidence
                                                                  k w))))
                                                    (get-nodes g)))))])
                           (get-nodes g)))))

(defn filter-edges
  "Filter for the edges of a graph"
  [g pred]
  (assoc g :incidences
         (into {}
               (map (fn [[n is]]
                      [n (into #{} (filter pred is))])
                    (:incidences g)))))

(defn clear-edges
  "Clear all edges"
  [g]
  (assoc g :incidences {}))

(defn map-edges
  "Map f to all edges of g"
  [f g]
  (assoc g :incidences
         (into {}
               (map (fn [[n i]]
                      [n (into #{}
                               (map f i))])
                    (:incidences g)))))



