(ns clj-stacktrace.repl
  (:use clj-stacktrace.core)
  (:require [clj-stacktrace.utils :as utils]))

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
    (if (utils/re-match? #"^clojure\." (:class elem))
      :cyan
      :blue)
    (cond (nil? (:ns elem)) :yellow
          (utils/re-match? #"^(user|repl)" (:ns elem)) :yellow
          (utils/re-match? #"^clojure\." (:ns elem)) :magenta
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

(defn pst-class-on [on color? class]
  (.append on (colored color? :red (str (.getName class) ": ")))
  (.flush on))

(defn pst-message-on [on color? message]
  (.append on (colored color? :red message))
  (.append on "\n")
  (.flush on))

(defn pst-elem-str
  [color? parsed-elem print-width]
  (colored color? (elem-color parsed-elem)
           (str (utils/rjust print-width (source-str parsed-elem))
                " " (method-str parsed-elem))))

(defn pst-elems-on
  [on color? parsed-elems & [source-width]]
  (let [print-width (+ 6 (or source-width
                             (utils/fence
                              (sort
                               (map #(.length %)
                                    (map source-str parsed-elems))))))]
    (doseq [parsed-elem parsed-elems]
      (.append on (pst-elem-str color? parsed-elem print-width))
      (.append on "\n")
      (.flush on))))

(defn pst-caused-by-on
  [on color?]
  (.append on (colored color? :red "Caused by: "))
  (.flush on))

(defn- pst-cause-on
  [on color? exec source-width]
  (pst-caused-by-on on color?)
  (pst-class-on on color? (:class exec))
  (pst-message-on on color? (:message exec))
  (pst-elems-on on color? (:trimmed-elems exec) source-width)
  (if-let [cause (:cause exec)]
    (pst-cause-on on color? cause source-width)))

(defn find-source-width
  "Returns the width of the longest source-string among all trace elems of the
  excp and its causes."
  [excp]
  (let [this-source-width (utils/fence
                           (sort
                            (map #(.length %)
                                 (map source-str (:trace-elems excp)))))]
    (if-let [cause (:cause excp)]
      (max this-source-width (find-source-width cause))
      this-source-width)))

(defn pst-on [on color? e]
  "Prints to the given Writer on a pretty stack trace for the given exception e,
  ANSI colored if color? is true."
  (let [exec         (parse-exception e)
        source-width (find-source-width exec)]
    (pst-class-on on color? (:class exec))
    (pst-message-on on color? (:message exec))
    (pst-elems-on on color? (:trace-elems exec) source-width)
    (if-let [cause (:cause exec)]
      (pst-cause-on on color? cause source-width))))

(defn pst
  "Print to *out* a pretty stack trace for an exception, by default *e."
  [& [e]]
  (pst-on *out* false (or e *e)))

(defn pst-str
  "Like pst, but returns a string instead of printing that string to *out*"
  [& [e]]
  (let [sw (java.io.StringWriter.)]
    (pst-on sw false (or e *e))
    (str sw)))

(defn pst+
  "Like pst, but with ANSI terminal color coding."
  [& [e]]
  (pst-on *out* true (or e *e)))
