Clojure, Graphs and Swing
=========================

6.3.2010

I’ve been looking at [Clojure](http://clojure.org/) for a short while now, and when I decided I needed to build a little graphical viewer for graphs, I thought it would be a great little project to get to know Clojure a little bit.


In this text I’ll introduce Clojure and take a closer look at a couple of things I enjoyed a lot when writing my small application. I won’t go into too much detail; there’s plenty of guides and documentation for all the features of Clojure available online. You should check out the videos of Rich Hickey’s and others’ talks on Clojure in places like [InfoQ](http://www.infoq.com/author/Rich-Hickey) or [blip.tv](http://clojure.blip.tv/) to get started, or just head straight over to [clojure.org](http://clojure.org/), if you would like to know more.

Clojure
-------

Clojure is a modern Lisp dialect that builds on the itellectual foundation of Scheme and Common Lisp, adding many features they are lacking in today’s world. In addition to the ubiquitous list data type, Clojure offers vectors, maps and sets and includes them as a part of the homoiconic language syntax. In true functional programming style all values are immutable and sequences can be lazy. One of the most interesting features of the language is it’s built in support for concurrency implemented by using software transactional memory for the core data structures.

Clojure compiles to JVM bytecode and is hosted on the JVM, and often times it [gets mentioned](http://blog.thinkrelevance.com/2008/8/4/java-next-common-ground) alongside other JVM newcomers such as Scala and Groovy. The fact that it runs on the JVM gives the language access to all the features and libraries available from Java, and Clojure makes access to all of it simple through easy to use language constructs.

One of the things I liked about Clojure while working with it is the rich set of data structures it provides. Having worked with Common Lisp in the past, a modern collection of data structures provided by the language feels a refreshing change. Coming from a procedural/object oriented background, the maps are naturally the data structure I tend to use the most, and Clojure makes them a pleasure to use.

The immutable values give a new twist to working with data structures, though. Instead of carrying around the data you are using everywhere, you need to find clever ways of creating copies of what you have with the changes you need. Luckily Clojure makes this incredibly easy. I believe a good example of this is the function add-edge in pparkkin.weighed-graph, where a copy is created of a graph, but one of the incidence lists is extended with a single new incidence. (Many thanks to Jeffrey Straszheim and his [clojure.contrib.graph](http://richhickey.github.com/clojure-contrib/graph-api.html) library, without which I would never have figured out how to manipulate graphs.)

Refs and Watches
----------------

One of the major features of Clojure is it’s [support for concurrency and concurrent programming](http://clojure.org/concurrent_programming), which is built right into the language itself. All of Clojure’s core data structures are mutable and so also sharable safely between threads. State is managed through transactional mutable references to objects, mainly refs, atoms and agents.

To get my graph viewer to keep up to date on changes to the graph it’s displaying, the graph is wrapped in a ref, and any changes made are made through the ref. The viewer adds a watch to the ref that calls a function to update the graphical view everytime the ref is changed. Reading in the viewer is done by simply dereferencing the ref to make a local immutable reference for drawing out the graph at that point in time. Using a ref and watches in the viewer makes it easy to keep my graph data structure code clean and separated completely from my viewer code.

Changes to the ref are made inside a dosync block that makes sure the ref stays in a consistent state and that the changes are atomic.

Swing and Java Interop
----------------------

The viewer GUI is written using [Swing](http://en.wikipedia.org/wiki/Swing_(Java\)), Sun’s widget toolkit for Java. [Using Java libraries is made easy in Clojure](http://clojure.org/java_interop) and included is support for calling methods, creating objects, extending classes and anything you might need to work with tools written in Java.

In the graph viewer I wrote functions that manipulate and return the objects I need for the GUI: The JFrame object is created and then manipulated in a doto block in the graph-frame function. For the JPanel object I use a proxy object to extend the class with my own methods for drawing the graph on the panel. The drawing functions take a Graphics object that they draw on, and the function definitions include type hints to avoid the performance hits that would otherwise be incurred from using reflection to dispatch the method.

Using Java objects from Clojure reminds me of how objects are used in the prototypal object model in JavaScript, and at least for building graphical user interfaces with Swing, it feels much more natural to me than working with traditional object oriented GUI toolkits I have worked with.

Using the Graph Viewer
----------------------

The graph viewer is best used from the REPL. That way it can be thought of as a kind of interactive environment for experimenting with graphs. The code is split into separate pieces that can be used (to an extent) independent of each other. The module for working with weighted graphs does not require the point-graph namespace to be useful, and the point graph functions work perfectly even without the viewer. The viewer is somewhat tied to the point graphs.

The following is a quick REPL session that shows the basics of what you can do. For the rest, you should consult the source code. (For this to work the files need to be in your classpath.)

    user> (use '(pparkkin weighted-graph))
    nil
    user> (def g (struct weighted-directed-graph))
    #'user/g
    user> (use '(pparkkin point-graph graph-view))
    nil
    user> (def g (random-fill g 50 (:width default-settings)
                                   (:height default-settings)))
    #'user/g
    user> (def g (ref g))
    #'user/g
    user> (def s (open-frame g))
    #'user/s
    user> (dosync (alter g connect-neighbors 100))
    {:nodes #{{:x 29, :y 268} {:x 338, :y 257} {:x 141, :y 155}
    ...
    user> (import '(java.awt Color))
    java.awt.Color
    user> (dosync (alter s assoc :vertex-fill-color (fn [n] Color/RED)))
    {:width 482, :height 348, :background-color #<Color
    ...

After the above, you should be looking at something roughly like the screenshot below.

![](https://lh3.googleusercontent.com/-ArO3_3GJkIc/S5IHGR9btkI/AAAAAAAAAyc/VJY90HdoWm8/s800/Screenshot-Graph%252520View.png)

Conclusions
-----------

In this text I introduced my newest favorite programming Language, Clojure, and the two features that make it so great for me: it’s built in concurrency support and it’s easy access to Java libraries. The concurrency support let’s me handle working in a functional style and a non-functional style side by side in a clean and safe way, by controlling the way I can access mutable state in my programs. The Java support gives me access to the libraries and supporting tools I need to be able to write the programs I want to write.

I hope this will get you to try out Clojure if you haven’t yet, and if you want to try out my graph viewer program, feel free to do so.

Djikstra’s algorithm for shortest paths
=======================================

23.5.2010

Now that I have a rudimentary sandbox, that I can play with graphs in, I decided to start with the classic algorithm for shortest paths: [Djikstra’s algorithm](http://en.wikipedia.org/wiki/Dijkstra's_algorithm).

[The shortest path problem](http://en.wikipedia.org/wiki/Shortest_path_problem) is of course simple in itself: find the shortest path between a pair of vertices in graph, but the problem can be divided into several subproblems that have their own solutions and algorithms. The single-pair shortest path problem is to find the shortest path between a pair of vertices. The single-source shortest path problem is to find the shortest path from a source vertex to all other vertices in a graph. The single-destination shortest path problem is to find the shortest path from every other vertex in a graph to a single destination vertex. And finally the all-pairs shortest path problem is to find the shortest paths between all vertice pairs in a graph. Djikstra’s algorithm is a solution to the single-source shortest path problem.

[Djikstra’s algorithm](http://en.wikipedia.org/wiki/Dijkstra's_algorithm) keeps a distance to each vertex from the source, or root, vertex as it goes through the graph. At first the distance for the root vertex is set to 0 and to all others are set to infinity. For each iteration of the algorithm the vertex with the lowest distance that has not been visited yet is picked, and all it’s neighbors are examined. Each neighbors distance is updated if it hasn’t been visited yet, or if it’s distance would be less if going through the vertex under examination. When all vertices are visited, we’re done.

Although the algorithm is simple enough to understand, and would likely be fairly simple to implement too in a procedural language, translating it to Clojure turned out to be tricky. I decided to create a separate list of distance structures that I use to store the distances and paths to each node, and in the end just filter out from the original graph the edges not included in the paths stored in the distance list. It took me a while before I could wrap my head around how I should work with the graph and the distance list at the same time, and update the distances at each iteration, but in the end I think the best way was to just try to focus on building the distance list and not worry about the graph itself too much. I’m not sure I understand it even now, but it seems to work in the simple cases at least. It’s a good start to improve on.

To try it out, follow the steps in the previous text to generate a new random graph, and after that do the following:

    user> (use '(pparkkin djikstra))
    nil
    user> (def r (first (:nodes @g)))
    #'user/r
    user> (def dg (djikstra @g r))
    #'user/dg
    user> (def dg (ref dg))
    #'user/dg
    user> (def ds (open-frame dg))
    #'user/ds
    user> (dosync (alter ds assoc :vertex-fill-color (fn [n]
                                                   (if (= n r)
                                                    Color/RED
                                                    Color/BLACK))))
    {:width 482, :height 348, :background-color #<Color
    ...

You should end up with something like this:

![](https://lh6.googleusercontent.com/-hlkklDV2dM8/TAIHdIBHpvI/AAAAAAAAAyc/pW7_5Wq0x_Y/s800/djikstra-screenshot-2.png)


