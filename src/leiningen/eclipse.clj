(ns leiningen.eclipse
  "Create Eclipse project descriptor files."
  (:import [java.io File]
           [java.util.regex Pattern])
  (:use [clojure.contrib.duck-streams :only [with-out-writer]]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.prxml :only [prxml *prxml-indent*]]
        [clojure.contrib.str-utils :only [re-sub]]
        [leiningen.classpath :only (get-classpath)]
        [leiningen.deps :only [deps]]
        [leiningen.jar :only (unix-path)]))

;; copied from jar.clj
(defn- trim-leading-str
  [s to-trim]
  (re-sub (re-pattern (str "^" (Pattern/quote to-trim))) "" s))

(defn- directory?
  [arg]
  (.isDirectory (File. arg)))

(defn- print-classpath
  "Print .classpath to *out*."
  [project]
  (let [root (str (unix-path (:root project)) \/)
        noroot #(trim-leading-str (unix-path (str %)) root)]
    (prxml
     [:decl!]
     [:classpath
      [:classpathentry
       {:kind "con" :path "org.eclipse.jdt.launching.JRE_CONTAINER"}]
      (if (directory? (:java-source-path project))
        [:classpathentry {:kind "src" :path (noroot (:java-source-path project))}])
      [:classpathentry
       {:kind "output" :path (noroot (:compile-path project))}]
      (for [path (get-classpath project)]
        [:classpathentry
         (if (.isDirectory (File. (str path)))
           {:kind "src" :path (noroot path)}
           {:kind "lib" :path (noroot path)})])])))

(defn- print-project
  "Print .project to *out*."
  [project]
  (prxml
   [:decl!]
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
    (with-out-writer (file (:root project) ".classpath")
      (print-classpath project))
    (println "Created .classpath")
    (with-out-writer (file (:root project) ".project")
      (print-project project))
    (println "Created .project")))
