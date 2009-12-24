`clj-stacktrace` is a library for creating more readable stacktraces in Clojure programs.

For example, to print a nice stack trace in a REPL:

    => (use 'clj-stacktrace.repl)
    => ("foo")
    java.lang.ClassCastException: java.lang.String
    user=> (pst)  ;instead of (.printStackTrace *e) 
    java.lang.ClassCastException: java.lang.String
             Compiler.java:4163 clojure.lang.Compiler.eval
                  core.clj:1497 clojure.core/eval
                   main.clj:148 clojure.main/repl[fn]
                   main.clj:145 clojure.main/repl
                RestFn.java:876 clojure.lang.RestFn.invoke
                repl_ln.clj:233 clojure.contrib.repl-ln/repl
                RestFn.java:402 clojure.lang.RestFn.invoke
                    user.clj:71 user/eval
             Compiler.java:4152 clojure.lang.Compiler.eval
             Compiler.java:4480 clojure.lang.Compiler.load
                    RT.java:327 clojure.lang.RT.loadResourceScript
                    RT.java:312 clojure.lang.RT.loadResourceScript
                    RT.java:308 clojure.lang.RT.maybeLoadResourceScript
                    RT.java:446 clojure.lang.RT.doInit
                    RT.java:286 clojure.lang.RT.<clinit>
              Namespace.java:31 clojure.lang.Namespace.<init>
             Namespace.java:116 clojure.lang.Namespace.findOrCreate
                   main.java:21 clojure.main.<clinit>
               (Unknown Source) java.lang.Class.forName0
                 Class.java:164 java.lang.Class.forName
          ConsoleRunner.java:69 jline.ConsoleRunner.main
    Caused by: java.lang.String
                       repl-1:1 user/eval
             Compiler.java:4152 clojure.lang.Compiler.eval

In stack traces printed by `pst`:

* Java methods are described with the usual `name.space.ClassName.methodName` convention and Clojure functions with their own `name.space/function-name` convention.
* Anonymous clojure functions are denoted by adding an `[fn]` to their enclosing, named function.
* "Caused by" cascades are shown as in regular java stack traces.
* Elements are vertically aligned for better readability.
* Printing is directed to `*out*`.

If you want to direct the printing to somewhere other than `*out*`, either use `pst-on` to specify the output location or `pst-str` to capture the printing as a string.

The library also offers an API for programatically 'parsing' exceptions. This API is used internal for `pst` and can be used to e.g. improve development tools. Try for example:

    => (use '(clj-stacktrace core))
    => (try
         ("nofn")
         (catch Exception e
           (parse-exception e)))

License
-------

Copyright 2009 Mark McGranaghan and released under an MIT license.
