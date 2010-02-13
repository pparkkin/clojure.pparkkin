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

(defn add-node
  "Add a node to a graph"
  [g n]
  (assoc g :nodes (conj (:nodes g) n)))

(defn add-nodes
  "Add a collection of nodes to a graph"
  ([g ns]
     (assoc g :nodes (clojure.set/union (:nodes g) (set ns))))
  ([g n f]
     (add-nodes g (map f (range n)))))

;; Note on removing nodes: "Sets support 'removal' with disj"

(defn clear-nodes
  "Clear all nodes"
  [g]
  (assoc g :nodes #{}))

(defn get-incidences
  "Get the incidences of a node"
  [g n]
  ((:incidences g) n))

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
                           (:nodes g)))))

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
                                 (into #{}
                                       (filter (fn [x]
                                                 (not (nil? x)))
                                               (map (fn [k]
                                                      (let [w (f n k)]
                                                        (when w
                                                          (struct incidence
                                                                  k w))))
                                                    (:nodes g)))))])
                           (:nodes g)))))

(defn clear-edges
  "Clear all edges"
  [g]
  (assoc g :incidences {}))



