(ns leiningen.eclipse
  "Create Eclipse project descriptor files."
  (:use [clojure.contrib.duck-streams :only [with-out-writer]])
  (:use [clojure.contrib.java-utils :only [file]])
  (:use [clojure.contrib.prxml :only [prxml *prxml-indent*]])
  (:use [clojure.contrib.str-utils :only [re-sub]])
  (:use [leiningen.deps :only [deps]])
  (:import [java.io File])
  (:import [java.util.regex Pattern]))

;; copied from jar.clj
(defn- unix-path
  [path]
  (.replaceAll path "\\\\" "/"))

;; copied from jar.clj
(defn- trim-leading-str
  [s to-trim]
  (re-sub (re-pattern (str "^" (Pattern/quote to-trim))) "" s))

(defn- directory?
  [arg]
  (.isDirectory (File. arg)))

(defn- list-libraries
  [project]
  (map #(.getPath %) (.listFiles (File. (:library-path project)))))

(defn- create-classpath
  "Print .classpath to *out*."
  [project]
  (let [root (str (unix-path (:root project)) \/)
        noroot  #(trim-leading-str (unix-path %) root)
        [resources-path compile-path source-path test-path]
        (map noroot (map project [:resources-path
				  :compile-path
				  :source-path
				  :test-path]))]
    (prxml [:decl!]
	   [:classpath
	    (if (directory? source-path)
	      [:classpathentry {:kind "src"
				:path source-path}])
	    (if (directory? resources-path)
		[:classpathentry {:kind "src"
				  :path resources-path}])
	    (if (directory? test-path)
	      [:classpathentry {:kind "src"
				:path test-path}])
	    [:classpathentry {:kind "con"
			       :path "org.eclipse.jdt.launching.JRE_CONTAINER"}]
	    (for [library (list-libraries project)]
	      [:classpathentry {:kind "lib"
				:path (noroot library)}])
	    [:classpathentry {:kind "output"
			       :path compile-path}]
	    ])))

(defn- create-project
  "Print .project to *out*."
  [project]
  (prxml [:decl!]
	 [:projectDescription
	  [:name (:name project)]
	  [:comment (:description project)]
	  [:projects]
	  [:buildSpec
	   [:buildCommand
	    [:name "ccw.builder"]
	    [:arguments]]
	   [:buildCommand
	    [:name "org.eclipse.jdt.core.javabuilder"]
	    [:arguments]]]
	  [:natures
	   [:nature "ccw.nature"]
	   [:nature "org.eclipse.jdt.core.javanature"]]]))

(defn eclipse
  "Create Eclipse project descriptor files."
  [project]
  (deps project)
  (binding [*prxml-indent* 2]
    (with-out-writer
      (file (:root project) ".classpath")
      (create-classpath project))
    (println "Created .classpath")
    (with-out-writer
      (file (:root project) ".project")
      (create-project project))
    (println "Created .project")))
