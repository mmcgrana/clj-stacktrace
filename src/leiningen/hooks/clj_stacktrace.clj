(ns leiningen.hooks.clj-stacktrace
  (:use [leiningen.compile :only [eval-in-project]]
        [robert.hooke :only [add-hook]]))

(defn- hook-form [form color?]
  (let [pst (if color?
              'clj-stacktrace.repl/pst+
              'clj-stacktrace.repl/pst)]
    `(do (if-let [add-hook# (resolve '~'robert.hooke/add-hook)]
           (add-hook# (resolve '~'clojure.stacktrace/print-cause-trace)
                      (fn [original# & args#]
                        (apply @(resolve '~pst) args#)))
           (println "clj-stacktrace needs robert.hooke dependency"))
         ~form)))

(defn- add-stacktrace-hook [eval-in-project project form & [h s init]]
  (eval-in-project project (hook-form form (:color (:clj-stacktrace project)))
                   h s `(do (try (require '~'robert.hooke)
                                 (require '~'clj-stacktrace.repl)
                                 (catch Exception _#))
                            ~init)))

(add-hook #'eval-in-project add-stacktrace-hook)
