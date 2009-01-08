(ns clj-backtrace.repl-test
  (:use clj-unit.core clj-backtrace.repl))

(deftest "pst, pst-str"
  (try (first (lazy-cons (/) :rest))
    (catch Exception e
      (assert-that (pst-str e))
      (binding [*e e]
        (assert-that (pst-str))))))
