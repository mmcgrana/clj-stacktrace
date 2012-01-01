(ns clj-stacktrace.repl-test
  (:use clojure.test
        clj-stacktrace.utils
        clj-stacktrace.repl
        midje.sweet))

(defmacro with-cascading-exception
  "Execute body in the context of a variable bound to an exception instance
  that includes a caused-by cascade."
  [binding-sym & body]
  `(try (first (lazy-seq (cons (/) nil)))
        (catch Exception e#
          (let [~binding-sym e#]
            ~@body))))

(deftest test-pst
  (with-cascading-exception e
    (is (with-out-str (pst e)))
    (binding [*e e]
      (is (with-out-str (pst))))))

(deftest test-pst-str
  (with-cascading-exception e
    (is (pst-str e))
    (binding [*e e]
      (is (pst-str)))))

(deftest test-pst+
  (with-cascading-exception e
    (is (with-out-str (pst+ e)))
    (binding [*e e]
      (is (with-out-str (pst+))))))

;; Color configuration tests

(defn- starts-colored [color]
  #(.startsWith % (color-codes color)) )

(fact "defaults to red errors" 
  (with-cascading-exception ex 
    (with-out-str (pst+ ex))) => (starts-colored :red))

(against-background [(around :contents (do
                                         (configure-colors {:error-color :blue})
                                         ?form
                                         (reset-color-configuration)))]
  (fact "colors are configurable"
    (with-cascading-exception ex 
      (with-out-str (pst+ ex))) => (starts-colored :blue)))

(fact "then, can reset the colors to defaults"
  (with-cascading-exception ex 
    (with-out-str (pst+ ex))) => (starts-colored :red))

;; Valid configuration?

(fact "must pass valid config keys"
  (configure-colors {:ERROR-COLOR :red}) => (throws Exception)
  (configure-colors {:error-color :red}) =not=> (throws Exception))

(fact "must pass valid config values"
  (configure-colors {:error-color :purple}) => (throws Exception)
  (configure-colors {:error-color :red}) =not=> (throws Exception))

