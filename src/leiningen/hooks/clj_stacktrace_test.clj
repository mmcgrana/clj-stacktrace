(ns leiningen.hooks.clj-stacktrace-test
  (:use [leiningen.compile :only [eval-in-project]]
        [robert.hooke :only [add-hook]]))

(defn- hook-form [form project]
  `(do (alter-var-root (resolve '~'clojure.stacktrace/print-cause-trace)
                       (constantly (fn [e#]
                                     (@(resolve '~'project-pst) e#
                                      ~(:clj-stacktrace project)))))
       ~form))

(defn- add-stacktrace-hook [eval-in-project project form & [h s init]]
  (eval-in-project project (hook-form form project)
                   h s `(do (try (require '~'clj-stacktrace.repl)
                                 (require '~'clojure.stacktrace)
                                 (catch Exception _#))
                            ~init)))

(add-hook #'eval-in-project add-stacktrace-hook)
