(ns clj-stacktrace.repl
  (:use [clj-stacktrace.core :only [parse-exception]]
        [clj-stacktrace.utils :only [omit-frames fence rjust]]))

(def color-codes
  {:red "\033[31m"
   :green "\033[32m"
   :yellow "\033[33m"
   :blue "\033[34m"
   :magenta "\033[35m"
   :cyan "\033[36m"
  
   :red-bg "\033[41m"
   :green-bg "\033[42m"
   :yellow-bg "\033[43m"
   :blue-bg "\033[44m"
   :magenta-bg "\033[45m"
   :cyan-bg "\033[46m"})

(def ^{:private true} default-colors {:error-color :red
                                      :user-code-color :green
                                      :repl-color :yellow
                                      :java-color :blue
                                      :clojure-color :magenta
                                      :clojure-java-color :cyan})

(defn- colored [color? color text color-overrides]
  (let [colors (merge default-colors color-overrides)]
    (if color?
      (str (color-codes (colors color)) text "\033[39m")
      text)))

(defn- elem-color
  "Returns a keyword identifying the color appropriate for the given trace elem.
  :clojure-java-color Java elems in clojure.*
  :java-color         Any other Java elems
  :repl-color         Any fn in the user or repl* namespaces (i.e. entered at REPL)
  :clojure-color      Any fn in clojure.* (e.g. clojure.core, clojure.contrib.*)
  :user-code-color    Anything else - i.e. Clojure libraries and app code."
  [elem]
  (if (:java elem)
    (if (re-find #"^clojure\." (:class elem))
      :clojure-java-color 
      :java-color)
    (cond (nil? (:ns elem)) :repl-color 
          (re-find #"^(user|repl)" (:ns elem)) :repl-color 
          (re-find #"^clojure\." (:ns elem)) :clojure-color 
          :else :user-code-color )))

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

(defn pst-class-on [^java.io.Writer on color? ^Class class color-overrides]
  (.append on ^String (colored color? :error-color (str (.getName class) ": ") color-overrides))
  (.flush on))

(defn pst-message-on [^java.io.Writer on color? message color-overrides]
  (.append on ^String (colored color? :error-color message color-overrides))
  (.append on "\n")
  (.flush on))

(defn pst-elem-str
  [color? parsed-elem print-width color-overrides]
  (colored color? (elem-color parsed-elem)
           (str (rjust print-width (source-str parsed-elem))
                " " (method-str parsed-elem))
           color-overrides))

(defn pst-elems-on
  [^java.io.Writer on color? parsed-elems & [source-width color-overrides]]
  (let [print-width (+ 6 (or source-width
                             (fence (sort (for [elem parsed-elems]
                                            (count (source-str elem)))))))]
    (doseq [parsed-elem parsed-elems]
      (.append on ^String (pst-elem-str color? parsed-elem print-width color-overrides))
      (.append on "\n")
      (.flush on))))

(defn pst-caused-by-on
  [^java.io.Writer on color? color-overrides]
  (.append on ^String (colored color? :error-color "Caused by: " color-overrides))
  (.flush on))

(defn- pst-cause-on
  [^java.io.Writer on exec {:keys [source-width omit color?]} color-overrides]
  (pst-caused-by-on on color? color-overrides)
  (pst-class-on on color? (:class exec) color-overrides)
  (pst-message-on on color? (:message exec) color-overrides)
  (pst-elems-on on color? (omit-frames (:trimmed-elems exec) omit)
                source-width color-overrides)
  (if-let [cause (:cause exec)]
    (pst-cause-on on color? cause source-width color-overrides)))

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
        color?       (or color? (:color? opts) (:test-color opts))
        color-overrides   (select-keys opts (keys default-colors))]
    (pst-class-on on color? (:class exec) color-overrides)
    (pst-message-on on color? (:message exec) color-overrides)
    (pst-elems-on on color? (omit-frames (:trace-elems exec) omit) source-width color-overrides)
    (if-let [cause (:cause exec)]
      (pst-cause-on on cause
                    (assoc opts
                      :source-width source-width
                      :color? color?)
                    color-overrides))))

(defn pst
  "Print to *out* a pretty stack trace for an exception, by default *e."
  [& [e & {:as opts}]]
  (pst-on *out* (or e *e) opts))

(defn pst+
  "Like pst, but with ANSI terminal color coding."
  [& [e & {:as opts}]]
  (pst-on *out* (or e *e) (assoc opts :color? true)))