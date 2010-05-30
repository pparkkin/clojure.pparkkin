(ns
    #^{:author "Paavo Parkkinen"
       :doc "An environment for visually experimenting with graphs"}
  pparkkin.graph-view
  (:import
   (javax.swing JFrame JPanel
                BorderFactory)
   (java.awt Color Dimension Graphics)
   (java.awt.event ComponentListener))
  (:use
   (pparkkin weighted-graph)
   (clojure.contrib swing-utils)))

;; Settings for the width, the height and the colors used in a window
;; For example values see default-settings below
(defstruct view-settings
  :width  ;; width of the graph view
  :height ;; height of the graph view
  :background-color ;; background color of graph view
  :vertex-diameter ;; size of vertices in graph view
  :vertex-fill-color ;; fill color of vertices in graph view
  :vertex-outline-color ;; outline color of vertices in graph view
  :edge-color) ;; color of edges in graph view
(def default-settings
     (struct-map view-settings
       :width  480
       :height 320
       :background-color Color/WHITE
       :vertex-diameter      (fn [vertex] 10)
       :vertex-fill-color    (fn [vertex] Color/BLACK)
       :vertex-outline-color (fn [vertex] Color/GRAY)
       :edge-color           (fn [edge] Color/BLACK)))


(defn draw-edge
  "Draw an edge on a Graphics panel."
  [#^Graphics g nf vt s]
  (let [nt (:to vt)
        from-offset (/ ((:vertex-diameter s) nf) 2)
        to-offset   (/ ((:vertex-diameter s) nt) 2)]
    (.setColor g ((:edge-color s) vt))
    (.drawLine g (+ (:x nf) from-offset) (+ (:y nf) from-offset)
                 (+ (:x nt) to-offset)   (+ (:y nt) to-offset))))

(defn draw-edges
  "Draw all the edges from a vertex on a Graphics panel."
  [#^Graphics g vf incidences s]
  (doseq [v incidences]
    (draw-edge g vf v s)))

(defn draw-vertex
  "Draw a vertex on a Graphics panel."
  [#^Graphics g v s]
  (.setColor g ((:vertex-fill-color s) v))
  (.fillOval g
             (:x v) (:y v)
             ((:vertex-diameter s) v) ((:vertex-diameter s) v))
  (.setColor g ((:vertex-outline-color s) v))
  (.drawOval g
             (:x v) (:y v)
             ((:vertex-diameter s) v) ((:vertex-diameter s) v)))

(defn draw-graph
  "Draw a graph on a Graphics panel.
  The graph and settings are assumed to be refs."
  [#^Graphics g graph settings]
  (let [graph-snapshot @graph     ; grab a snapshot of the graph
        s              @settings] ; grab a snapshot of the settings
    (doseq [n (:nodes graph-snapshot)]
      ;; draw the edges first so they don't show on top of the vertices
      (draw-edges g n (get-incidences graph-snapshot n) s))
    (doseq [n (:nodes graph-snapshot)]
      (draw-vertex g n s))))

(defn graph-panel-proxy
  "A proxy JPanel object to display a graph"
  [graph settings]
  (proxy [JPanel] []
    (getPreferredSize []
      (new Dimension
           (settings :width)
           (settings :height)))
    (paintComponent [g]
      (proxy-super paintComponent g)
      (draw-graph g graph settings))))

(defn graph-panel
  "A panel to display a graph"
  [graph settings]
  (let [panel (graph-panel-proxy graph settings)]
    (add-watch graph
               :graph-repaint ; this key is required because I have
                              ; multiple watches
               (fn [key ref old-state new-state]
                 (.repaint panel)))
    (add-watch settings
               :settings-repaint ; this key is required because I have
                                 ; multiple watches
               (fn [key ref old-state new-state]
                 (.setBackground panel (:background-color new-state))
                 (.repaint panel)))
    
    (doto panel
      (.setBorder (BorderFactory/createLineBorder Color/BLACK))
      (.setBackground (:background-color @settings))
;      (.addMouseListener
;       (proxy [MouseAdapter] []
;        (mousePressed [e]
;          (move-vertex v panel (.getX e) (.getY e)))))
;      (.addMouseMotionListener
;       (proxy [MouseAdapter] []
;        (mouseDragged [e]
;          (move-vertex v panel (.getX e) (.getY e)))))
      )))

(defn add-component-listener
  "Adds a component listener to component."
  [component f & args]
  (let [listener (proxy [ComponentListener] []
                   (componentResized [event] (apply f event args))
                   (componentMoved [event] (apply f event args))
                   (componentShown [event] (apply f event args)))]
    (.addComponentListener component listener)
    listener))
                                     

(defn graph-frame
  "A frame to display a graph"
  [graph settings]
  (let [frame (new JFrame "Graph View")]
    (add-watch settings
               :settings-setSize ; this key is required because I have
                                 ; multiple watches
               (fn [key ref old-state new-state]
                 (.setSize frame
                           (:width new-state)
                           (:height new-state))))
    
    (doto frame
      (add-component-listener (fn [e]
                                (let [c (.getSource e)]
                                  (dosync
                                   (alter settings
                                          assoc
                                          :width (.getWidth c)
                                          :height (.getHeight c))))))
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
      (.add (graph-panel graph settings))
      (.pack)
      (.setLocationRelativeTo nil))))

(defn open-frame
  "Open up a window to display a graph"
  ([g]
     (open-frame g default-settings))
  ([g s]
     (let [settings (ref (merge default-settings s))]
       (do-swing
          (javax.swing.UIManager/setLookAndFeel
           (javax.swing.UIManager/getSystemLookAndFeelClassName))
          (.setVisible (graph-frame g settings) true))
       settings)))

;; Quick start:
(comment
  (use '(pparkkin weighted-graph))
  (def g (struct weighted-directed-graph))
  (use '(pparkkin point-graph graph-view))
  (def g (random-fill g 50 (:width default-settings)
                      (:height default-settings)))
  (def g (ref g))
  (def s (open-frame g))
  (dosync (alter g connect-neighbors 100))
  (import '(java.awt Color))
  (dosync (alter s assoc :vertex-fill-color (fn [n] Color/RED)))
  )
