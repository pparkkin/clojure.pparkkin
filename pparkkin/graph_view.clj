;; TODO:
;; + random fill
;; + autogenerate edges between close-by vertices
;; - autogenerate edges when a vertice is added/moved
;; - optimizations
;;   - (.repaint *canvas*) -calls to only repaint the changed area
;;   - r-trees to find near-by vertices
;; - code cleanup

(ns
    #^{:author "Paavo Parkkinen"
       :doc "An environment for visually experimenting with graphs"}
  parkkin.graph-view
  (:import
   (javax.swing JFrame JPanel
                BorderFactory)
   (java.awt Color Dimension Graphics)
   (java.awt.event ComponentListener)))

(defonce preferred-width 480)
(defonce preferred-height 320)

(defstruct settings
  :width
  :height
  :vertex-diameter
  :background-color
  :vertex-fill-color
  :vertex-outline-color
  :edge-color)
(def default-settings
     (struct-map settings
             :vertex-diameter 10
             :background-color Color/WHITE
             :vertex-fill-color Color/BLACK
             :vertex-outline-color Color/GRAY
             :edge-color Color/BLACK))


(defn draw-edge [#^Graphics g vf vt s]
  (let [offset (/ (:vertex-diameter s) 2)
        vt (:to vt)]
    (.setColor g (:edge-color s))
    (.drawLine g (+ (:x vf) offset) (+ (:y vf) offset)
                 (+ (:x vt) offset) (+ (:y vt) offset))))

(defn draw-edges [#^Graphics g vf incidences s]
  (doseq [v incidences]
    (draw-edge g vf v s)))

(defn draw-vertex [#^Graphics g v s]
  (println "draw-vertex") ; debug
  (.setColor g (:vertex-fill-color s))
  (.fillOval g (:x v) (:y v) (:vertex-diameter s) (:vertex-diameter s))
  (.setColor g (:vertex-outline-color s))
  (.drawOval g (:x v) (:y v) (:vertex-diameter s) (:vertex-diameter s)))

(defn draw-vertices
  "Draw a graph on a Graphics panel.
  The graph and settings are assumed to be refs."
  [#^Graphics g graph settings]
  (let [graph-snapshot (into {} @graph) ; create a snapshot of the graph
        s (into {} @settings)] ; create a snapshot of the settings
    (doseq [n (:nodes graph-snapshot)]
      (println s) ; debug
      (println n) ; debug
      (draw-vertex g n s)
      (draw-edges g n ((:incidences graph-snapshot) n) s))))

(defn graph-panel-proxy [graph settings]
  (proxy [JPanel] []
    (getPreferredSize []
      (new Dimension preferred-width preferred-height))
    (paintComponent [g]
      (proxy-super paintComponent g)
      ;(.clearRect g 0 0 (.getWidth this) (.getHeight this))
      (draw-vertices g graph settings))))

(defn graph-panel [graph settings]
  (let [panel (graph-panel-proxy graph settings)]
    ;; Define non-public functions for interacting with the panel
    ;(defn- repaint [] (.repaint panel))
    ;(defn- frame-width [] (.getWidth panel))
    ;(defn- frame-height [] (.getHeight panel))

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

(defn graph-frame [graph settings]
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
  "Open up a window to display a mesh graph"
  [g]
  (let [settings (ref (merge (struct settings)
                             default-settings))]
    (javax.swing.SwingUtilities/invokeLater
     (fn []
       (javax.swing.UIManager/setLookAndFeel
        (javax.swing.UIManager/getSystemLookAndFeelClassName))
       (.setVisible (graph-frame g settings) true)))
    settings))

;;; Things to try out:
;;; + open-frame works
;;; + change settings geometry -> frame resize
;;;   (dosync (alter s assoc :width 520))
;;; + frame resize -> change settings geometry
;;; + change settings colors -> panel repaint
;;;   (dosync (alter s assoc :edge-color Color/RED))

