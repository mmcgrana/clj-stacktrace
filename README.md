# clj-stacktrace

A library for creating more readable stacktraces in Clojure programs.

For example, to print a nice stack trace in a REPL:

    => (refer 'clj-stacktrace.repl)
    => ("foo")
    java.lang.ClassCastException: java.lang.String cannot be cast to clojure.lang.IFn (NO_SOURCE_FILE:0)
           Compiler.java:5440 clojure.lang.Compiler.eval
           Compiler.java:5391 clojure.lang.Compiler.eval
                core.clj:2382 clojure.core/eval
                 main.clj:183 clojure.main/repl[fn]
                 main.clj:204 clojure.main/repl[fn]
                 main.clj:204 clojure.main/repl
              RestFn.java:422 clojure.lang.RestFn.invoke
                 main.clj:262 clojure.main/repl-opt
                 main.clj:355 clojure.main/main
              RestFn.java:398 clojure.lang.RestFn.invoke
                 Var.java:361 clojure.lang.Var.invoke
                 AFn.java:159 clojure.lang.AFn.applyToHelper
                 Var.java:482 clojure.lang.Var.applyTo
                 main.java:37 clojure.main.main
    Caused by: java.lang.String cannot be cast to clojure.lang.IFn
             NO_SOURCE_FILE:2 user/eval100
           Compiler.java:5424 clojure.lang.Compiler.eval


In stack traces printed by `pst`:

* Java methods are described with the usual `name.space.ClassName.methodName` convention and Clojure functions with their own `name.space/function-name` convention.
* Anonymous clojure functions are denoted by adding an `[fn]` to their enclosing, named function.
* "Caused by" cascades are shown as in regular java stack traces.
* Elements are vertically aligned for better readability.
* Printing is directed to `*out*`.

If you want to direct the printing to somewhere other than `*out*`, either use `pst-on` to specify the output location or `pst-str` to capture the printing as a string.

The library also offers an API for programatically 'parsing' exceptions. This API is used internal for `pst` and can be used to e.g. improve development tools. Try for example:

```clj
(use 'clj-stacktrace.core)
(try
  ("nofn")
  (catch Exception e
    (parse-exception e)))
```

## Leiningen

If you use Leiningen, you can install clj-stacktrace on a per-user basis.

For Leiningen 2.x, add the following to `~/.lein/profiles.clj`:

```clj
{:user {:dependencies [[clj-stacktrace "0.2.5"]]
        :injections [(let [orig (ns-resolve (doto 'clojure.stacktrace require)
                                            'print-cause-trace)
                           new (ns-resolve (doto 'clj-stacktrace.repl require)
                                           'pst)]
                       (alter-var-root orig (constantly @new)))]}}
```

For Leiningen 1.x:

    $ lein plugin install clj-stacktrace 0.2.5

Add this to your `~/.lein/init.clj` file:

```clj
(try (require 'leiningen.hooks.clj-stacktrace-test)
     (def settings {:repl-options [:init (require 'clj-stacktrace.repl)
                                   :caught 'clj-stacktrace.repl/pst+]})
  (catch java.io.FileNotFoundException _))
```

The hook will enable clj-stacktrace to be used across all the projects
you work on in clojure.test and other things that use the
clojure.stacktrace library. The `:repl-options` settings will cause
clj-stacktrace to be used in the repl and swank tasks.

## License

Copyright © 2009-2012 Mark McGranaghan and contributors.

Released under an MIT license.
