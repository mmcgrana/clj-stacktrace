(defproject clj-stacktrace "0.2.2"
  :description "More readable stacktraces in Clojure programs."
  :url "http://github.com/mmcgrana/clj-stacktrace"
  :dev-dependencies [[org.clojure/clojure "1.2.1"]
                     [lein-clojars "0.6.0"]]
  :hooks [leiningen.hooks.clj-stacktrace-test]
  :repl-options [:caught clj-stacktrace.repl/pst+])
