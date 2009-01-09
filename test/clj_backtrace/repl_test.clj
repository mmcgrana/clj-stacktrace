(ns clj-backtrace.repl-test
  (:use clj-unit.core clj-backtrace.repl))

(defmacro with-cascading-exception
  "Execute body in the context of a variable bound to an exception instance
  that includes a caused-by cascade."
  [binding-sym & body]
  `(try (first (lazy-cons (/) :rest))
     (catch Exception e#
       (let [~binding-sym e#]
         ~@body))))

(deftest "pst, pst-str"
  (with-cascading-exception e
    (assert-that (pst-str e))
    (binding [*e e]
      (assert-that (pst-str)))))

(deftest "pst+"
  (with-cascading-exception e
    (assert-not=
      (pst-str e)
      (with-out-str (pst+ e)))
    (binding [*e e]
      (assert-that (with-out-str (pst+))))))