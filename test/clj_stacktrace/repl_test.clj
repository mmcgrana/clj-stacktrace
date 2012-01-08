(ns clj-stacktrace.repl-test
  (:use [clojure.test]
        [clj-stacktrace.repl]))

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

(deftest test-pst+
  (with-cascading-exception e
    (is (with-out-str (pst+ e)))
    (binding [*e e]
      (is (with-out-str (pst+))))))

(deftest test-omit
  (with-cascading-exception e
    (is (not (re-find #"repl-test" (with-out-str
                                     (pst e :omit #"repl-test")))))
    (is (not (re-find #"Compiler.java"
                      (with-out-str
                        (pst e :omit (fn [e]
                                       (= "Compiler.java" (:file e))))))))))

;; Color configuration tests
                                
(defn starts-with? [color s]
  (.startsWith s (color-codes color))) 

(deftest pst-uses-no-color
  (with-cascading-exception ex
    (is (not (starts-with? :red (with-out-str (pst ex)))))))

(deftest defaults-to-red-exceptions
  (with-cascading-exception ex
    (is (starts-with? :red (with-out-str (pst+ ex))))))

(deftest configure-colors
  (with-cascading-exception ex
    (is (starts-with? :blue (with-out-str (pst+ ex :error-color :blue ))))))