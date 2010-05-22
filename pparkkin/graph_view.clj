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
   (pparkkin weighted-graph)))

;; Settings for the width, the height and the colors used in a window
(defstruct view-settings
  :width
  :height
  :vertex-diameter
  :background-color
  :vertex-fill-color
  :vertex-outline-color
  :edge-color)
(def default-settings
     (struct-map view-settings
       :width 480
       :height 320
       :vertex-diameter 10
       :background-color Color/WHITE
       :vertex-fill-color Color/BLACK
       :vertex-outline-color Color/GRAY
       :edge-color Color/BLACK))


(defn draw-edge
  "Draw an edge on a Graphics panel."
  [#^Graphics g nf vt s]
  (let [offset (/ (:vertex-diameter s) 2)
        nt (:to vt)]
    (.setColor g
               (or (:color vt)
                   (:edge-color s)))
    (.drawLine g (+ (:x nf) offset) (+ (:y nf) offset)
                 (+ (:x nt) offset) (+ (:y nt) offset))))

(defn draw-edges
  "Draw all the edges from a vertex on a Graphics panel."
  [#^Graphics g vf incidences s]
  (doseq [v incidences]
    (draw-edge g vf v s)))

(defn draw-vertex
  "Draw a vertex on a Graphics panel."
  [#^Graphics g v s]
  (.setColor g
             (or (:fill-color v)
                 (:vertex-fill-color s)))
  (.fillOval g (:x v) (:y v) (:vertex-diameter s) (:vertex-diameter s))
  (.setColor g
             (or (:outline-color v)
                 (:vertex-outline-color s)))
  (.drawOval g (:x v) (:y v) (:vertex-diameter s) (:vertex-diameter s)))

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
      (.addComponentListener
       (proxy [ComponentListener] []
         (componentResized [e]
                           (let [c (.getSource e)]
                             (dosync
                              (alter settings
                                     assoc
                                     :width (.getWidth c)
                                     :height (.getHeight c)))))
         ;; Need to define componentMoved and -Shown or I get errors
         ;; about them being missing.
         (componentMoved [e])
         (componentShown [e])))
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
       (javax.swing.SwingUtilities/invokeLater
        (fn []
          (javax.swing.UIManager/setLookAndFeel
           (javax.swing.UIManager/getSystemLookAndFeelClassName))
          (.setVisible (graph-frame g settings) true)))
       settings)))

