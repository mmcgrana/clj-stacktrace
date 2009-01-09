(ns clj-backtrace.repl
  (:use (clj-backtrace core utils)))

(def *use-color* false)

(def color-codes
  {:red     "\033[31m"
   :green   "\033[32m"
   :yellow  "\033[33m"
   :blue    "\033[34m"
   :magenta "\033[35m"
   :cyan    "\033[36m"
   :default "\033[39m"})

(defn with-color
  "If *use-color* is bound to logical truth, returns text surrounded by 
  strings that will turn the text the given color and then reset the color
  to default. Otherwise returns the given string."
  [color text]
  (if *use-color*
    (str (color-codes color) text (color-codes :default))
    text))

(defn elem-color
  "Returns a symbol identifying the color appropriate for the given trace elem.
  :green   All Java elems
  :magenta Any fn in the user or repl* namespaces (i.e. entered at REPL)
  :blue    Any fn in clojure.* (e.g. clojure.core, clojure.contrib.*)
  :default Anything else - i.e. Clojure libraries and app code."
  [elem]
  (cond
    (:java elem)
      :green
    (or (nil? (:ns elem)) (re-match? #"^(user|repl)" (:ns elem)))
      :magenta
    (re-match? #"^clojure\." (:ns elem))
      :blue
    :else
      :default))

(defn source-str [parsed]
  (if (and (:file parsed) (:line parsed))
    (str (:file parsed) ":" (:line parsed))
    "(Unknown Source)"))

(defn clojure-method-str [parsed]
  (str (:ns parsed) "/" (:fn parsed) (if (:annon-fn parsed) "[fn]")))

(defn java-method-str [parsed]
  (str (:class parsed) "." (:method parsed)))

(defn method-str [parsed]
  (if (:java parsed) (java-method-str parsed) (clojure-method-str parsed)))

(defn print-trace-elems
  "Print a pretty stack trace for the parsed elems."
  [parsed-elems & [source-width]]
  (let [print-width
          (+ 6 (or source-width
                   (high (map (memfn length) (map source-str parsed-elems)))))]
    (doseq [parsed-elem parsed-elems]
      (println (with-color (elem-color parsed-elem)
                 (str (rjust print-width (source-str parsed-elem))
                      " " (method-str parsed-elem)))))))

(defn- pst-cause
  "Print a pretty stack trace for a parsed exception in a causal chain."
  [exec source-width]
  (println (with-color :red (str "Caused by: " (:message exec))))
  (print-trace-elems (:trimmed-elems exec) source-width)
  (if-let [cause (:cause exec)]
    (pst-cause cause source-width)))

(defn- find-source-width
  "Returns the width of the longest source-string among all trace elems of the 
  excp and its causes."
  [excp]
    (let [this-source-width
            (high (map (memfn length) (map source-str (:trace-elems excp))))]
      (if-let [cause (:cause excp)]
        (max this-source-width (find-source-width cause))
        this-source-width)))

(defn pst
  "Print to *out* a pretty stack trace for an exception, by default *e."
  [& [e]]
  (let [exec      (parse-exception (or e *e))
        source-width (find-source-width exec)]
    (println (with-color :red (:message exec)))
    (print-trace-elems (:trace-elems exec) source-width)
    (if-let [cause (:cause exec)]
      (pst-cause cause source-width))))

(defn pst-str
  "Like pst, but returns a string instead of printing that string to *out*"
  [& [e]]
  (with-out-str (pst (or e *e))))

(defn pst+
  "Experimenal. Like pst, but with ANSI terminal color coding.
  Prints ..."
  [& [e]]
  (binding [*use-color* true]
    (pst e)))
