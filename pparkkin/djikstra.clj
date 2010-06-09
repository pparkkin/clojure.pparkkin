(ns
    #^{:author "Paavo Parkkinen"
       :doc "Version of Djikstra's single source shortest path algorithm"}
  pparkkin.djikstra
  (:use (pparkkin weighted-graph)))

;; TODO: Make this not be an ugly hack

(defn cmp-with-inf
  "A simple function to compare two numbers.
  The nil value will always be > any other value,
  i.e. positive infinity"
  [a b]
  (cond
    (nil? a)
    1
    (nil? b)
    -1
    true
    (compare a b)))

(defn- cmp-distances
  "Compare two distance structures.
  Visited distances are always \"bigger\""
  [a b]
  (cond
    (:visited a)
    (if (:visited b) ; both visited
      (cmp-with-inf (:distance a)
                    (:distance b))
      1) ; a only visited
    (:visited b) ; b only visited
    -1
    true ; neither visited
    (cmp-with-inf (:distance a)
                  (:distance b))))

(defn- init-distance-list
  "Initialize a list of distance structures from a graph"
  [g r]
  (sort cmp-distances
        (map (fn [n]
               (hash-map
                :node n
                :visited false
                :distance (if (= n r)
                            0
                            nil)
                :path ()))
             (:nodes g))))

(defn- select-distance
  "Find the distance structure for a node"
  [n ds]
  (first
   (filter (fn [d]
             (= (:node d) n))
           ds)))

(defn- distances-from
  "Get distances from a node"
  [n g ds] ; [node graph distances]
  (let [is (get-incidences g n)    ; edges from the node
        nd (select-distance n ds)] ; the distance for the node
    (conj
     ;; go through edges from a node n
     (map (fn [k]
            ;; update the distance of the node at the other end
            ;; an edge:
            (let [kd (select-distance (:to k) ds)]
              ;; get the distance of the node, and
              ;; if the distance doesn't exist (is infinite, nil)
              ;; of if the distance is less through this edge,
              ;; update the distance, otherwise leave it alone
              (if (or (nil? kd)
                      (nil? (:distance kd))
                      (< (+ (:distance nd) (:weight k))
                         (:distance kd)))
                (hash-map
                 :node (:to k)
                 :visited false ; what if (:to k) has already been visited?
                 :path (conj (:path nd) k)
                 :distance (+ (:distance nd) (:weight k)))
                kd)))
          is)
     (assoc nd :visited true))))

(defn- merge-by
  "Merge two seqs using a supplied predicate to determine elements
  that are the same."
  [pred seq1 seq2]
  (loop [s1  (vec seq1)
         s2  (vec seq2)
         res (vector)]
    (cond
      (empty? s1)
      (concat res s2)
      
      (empty? s2)
      (concat res s1)
      
      true
      (let [matches (filter (fn [n]
                              (= (pred n)
                                 (pred (first s1))))
                            s2)]
        (if (empty? matches)
          ;; (first s1):lle ei löydy korviketta s2:sta
          (recur (rest s1) ; seuraava s1 käsittelyyn
                 s2 ; s2 ei muuttunut
                 (conj res (first s1))) ; (first s1) resultiin
          (recur (rest s1) ; seuraava s1 käsittelyyn
                 (remove (fn [n] (some #{n} matches)) s2)
                 (conj res (first matches)))))))) ; eka löydetty s2 tuloksiin

(defn- my-merge
  [lst new-lst]
  (sort cmp-distances
        (merge-by :node lst new-lst)))

(defn- distances
  [g r]
  (loop [ks (init-distance-list g r)
         k (first ks)]
    (if (or (:visited k) (nil? (:distance k)))
      (sort cmp-distances ks)
      (let [new-ks (my-merge ks
                             (distances-from (:node k)
                                             g
                                             ks))]
        (recur new-ks
               (first new-ks))))))

(defn- shortest-paths
  [g r]
  (into #{} (apply concat (map :path (distances g r)))))

(defn- trim-graph
  "Remove all but the provided incidences from a graph"
  [g paths]
  (filter-edges g (fn [n]
                    (contains? paths n))))

(defn djikstra
  "Return a new graph including only the edges comprising the shortest
  paths from r to every other node"
  [g r]
  (trim-graph g
              (shortest-paths g r)))

;; Quick start:
(comment
  (use '(pparkkin djikstra))
  (def r (first (:nodes @g)))
  (def dg (djikstra @g r))
  (def dg (ref dg))
  (def ds (open-frame dg))
  (dosync (alter ds assoc :vertex-fill-color (fn [n]
                                               (if (= n r)
                                                 Color/RED
                                                 Color/BLACK))))
  )
