(ns clj-backtrace.repl
  (:use (clj-backtrace core utils)))

(defn source-str [parsed]
  (if (and (:file parsed) (:line parsed))
    (str (:file parsed) ":" (:line parsed))
    "(Unknown Source)"))

(defn- clojure-method-str [parsed]
  (str (:ns parsed) "/" (:fn parsed) (if (:annon-fn parsed) "[fn]")))

(defn java-method-str [parsed]
  (str (:class parsed) "." (:method parsed)))

(defn- method-str [parsed]
  (if (:java parsed) (java-method-str parsed) (clojure-method-str parsed)))

(defn print-trace-elems
  "Print a pretty stack trace for the parsed elems."
  [parsed-elems & [source-width]]
  (let [print-width
          (+ 6 (or source-width
                   (high (map (memfn length) (map source-str parsed-elems)))))]
    (doseq [parsed-elem parsed-elems]
      (println (str (rjust print-width (source-str parsed-elem))
                    " " (method-str parsed-elem))))))

(defn- pst-cause
  "Print a pretty stack trace for a parsed exception in a causal chain."
  [exec source-width]
  (println (str "Caused by: " (:message exec)))
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
  "Print a pretty stack trace for an exception, by default *e."
  [& [e]]
  (let [exec      (parse-exception (or e *e))
        source-width (find-source-width exec)]
    (println (:message exec))
    (print-trace-elems (:trace-elems exec) source-width)
    (if-let [cause (:cause exec)]
      (pst-cause cause source-width))))

(defmacro with-pst
  "Wrap code in a guard that will print pretty stack traces instead of default
  Java traces on exceptions"
  [& body]
  `(try
     ~@body
     (catch Exception e# (pst e#))))