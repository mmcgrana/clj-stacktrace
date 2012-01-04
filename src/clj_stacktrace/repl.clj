(ns clj-stacktrace.repl
  (:use [clj-stacktrace.core :only [parse-exception]]
        [clj-stacktrace.utils :only [omit-frames fence rjust]]))

(def color-codes
  {:red     "\033[31m"
   :green   "\033[32m"
   :yellow  "\033[33m"
   :blue    "\033[34m"
   :magenta "\033[35m"
   :cyan    "\033[36m"
   :default "\033[39m"})

(defn- colored
  [color? color text]
  (if color?
    (str (color-codes color) text (color-codes :default))
    text))

(defn elem-color
  "Returns a symbol identifying the color appropriate for the given trace elem.
  :green   All Java elems
  :yellow  Any fn in the user or repl* namespaces (i.e. entered at REPL)
  :blue    Any fn in clojure.* (e.g. clojure.core, clojure.contrib.*)
  :magenta Anything else - i.e. Clojure libraries and app code."
  [elem]
  (if (:java elem)
    (if (re-find #"^clojure\." (:class elem))
      :cyan
      :blue)
    (cond (nil? (:ns elem)) :yellow
          (re-find #"^(user|repl)" (:ns elem)) :yellow
          (re-find #"^clojure\." (:ns elem)) :magenta
          :user-code :green)))

(defn source-str [parsed]
  (if (and (:file parsed) (:line parsed))
    (str (:file parsed) ":" (:line parsed))
    "(Unknown Source)"))

(defn clojure-method-str [parsed]
  (str (:ns parsed) "/" (:fn parsed) (if (:anon-fn parsed) "[fn]")))

(defn java-method-str [parsed]
  (str (:class parsed) "." (:method parsed)))

(defn method-str [parsed]
  (if (:java parsed) (java-method-str parsed) (clojure-method-str parsed)))

(defn pst-class-on [^java.io.Writer on color? ^Class class]
  (.append on ^String (colored color? :red (str (.getName class) ": ")))
  (.flush on))

(defn pst-message-on [^java.io.Writer on color? message]
  (.append on ^String (colored color? :red message))
  (.append on "\n")
  (.flush on))

(defn pst-elem-str
  [color? parsed-elem print-width]
  (colored color? (elem-color parsed-elem)
           (str (rjust print-width (source-str parsed-elem))
                " " (method-str parsed-elem))))

(defn pst-elems-on
  [^java.io.Writer on color? parsed-elems & [source-width]]
  (let [print-width (+ 6 (or source-width
                             (fence (sort (for [elem parsed-elems]
                                            (count (source-str elem)))))))]
    (doseq [parsed-elem parsed-elems]
      (.append on ^String (pst-elem-str color? parsed-elem print-width))
      (.append on "\n")
      (.flush on))))

(defn pst-caused-by-on
  [^java.io.Writer on color?]
  (.append on ^String (colored color? :red "Caused by: "))
  (.flush on))

(defn- pst-cause-on
  [^java.io.Writer on exec {:keys [source-width omit color?]}]
  (pst-caused-by-on on color?)
  (pst-class-on on color? (:class exec))
  (pst-message-on on color? (:message exec))
  (pst-elems-on on color? (omit-frames (:trimmed-elems exec) omit)
                source-width)
  (if-let [cause (:cause exec)]
    (pst-cause-on on color? cause source-width)))

(defn find-source-width
  "Returns the width of the longest source-string among all trace elems of the
  excp and its causes."
  [excp]
  (let [this-source-width (->> (:trace-elems excp)
                               (map (comp count source-str))
                               (sort) (fence))]
    (if-let [cause (:cause excp)]
      (max this-source-width (find-source-width cause))
      this-source-width)))

(defn pst-on
  "Prints to the given Writer on a pretty stack trace for the given exception e,
  ANSI colored if color? is true."
  [on e {:keys [omit color?] :as opts}]
  (let [exec         (parse-exception e)
        source-width (find-source-width exec)
        color? (or color? (:color? opts) (:test-color opts))]
    (pst-class-on on color? (:class exec))
    (pst-message-on on color? (:message exec))
    (pst-elems-on on color? (omit-frames (:trace-elems exec) omit) source-width)
    (if-let [cause (:cause exec)]
      (pst-cause-on on cause
                    (assoc opts
                      :source-width source-width
                      :color? color?)))))

(defn pst
  "Print to *out* a pretty stack trace for an exception, by default *e."
  [& [e & {:as opts}]]
  (pst-on *out* (or e *e) opts))

(defn pst+
  "Like pst, but with ANSI terminal color coding."
  [& [e & {:as opts}]]
  (pst-on *out* (or e *e) (assoc opts :color? true)))
