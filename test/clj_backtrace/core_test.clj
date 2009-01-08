(ns clj-backtrace.core-test
  (:use clj-unit.core (clj-backtrace core utils)))

(def cases-data
  [["foo.bar$biz__123" "invoke" "bar.clj" 456
    {:clojure true :ns "foo.bar" :fn "biz" :file "bar.clj" :line 456 :annon-fn false}]

   ["foo.bar$biz_bat__123" "invoke" "bar.clj" 456
    {:clojure true :ns "foo.bar" :fn "biz-bat" :file "bar.clj" :line 456 :annon-fn false}]

   ["foo.bar$biz_bat_QMARK___448" "invoke" "bar.clj" 456
    {:clojure true :ns "foo.bar" :fn "biz-bat?" :file "bar.clj" :line 456 :annon-fn false}]

   ["foo.bar$biz_bat_QMARK___448$fn__456" "invoke" "bar.clj" 456
     {:clojure true :ns "foo.bar" :fn "biz-bat?" :file "bar.clj" :line 456 :annon-fn true}]

   ["foo.bar$biz__123$fn__456" "invoke" "bar.clj" 789
    {:clojure true :ns "foo.bar" :fn "biz" :file "bar.clj" :line 789 :annon-fn true}]

   ["foo.bar_bat$biz__123" "invoke" "bar.clj" 456
    {:clojure true :ns "foo.bar-bat" :fn "biz" :file "bar.clj" :line 456 :annon-fn false}]

   ["user$eval__345" "invoke" nil -1
    {:clojure true :ns "user" :fn "eval" :file nil :line nil :annon-fn false}]

   ["clojure.lang.Var" "invoke" "Var.java" 123
    {:java true :class "clojure.lang.Var" :method "invoke" :file "Var.java" :line 123}]

   ["clojure.proxy.space.SomeClass" "someMethod" "SomeClass.java" 123
    {:java true :class "clojure.proxy.space.SomeClass" :method "someMethod" :file "SomeClass.java" :line 123}]

   ["some.space.SomeClass" "someMethod" "SomeClass.java" 123
    {:java true :class "some.space.SomeClass" :method "someMethod" :file "SomeClass.java" :line 123}]

   ["some.space.SomeClass$SomeInner" "someMethod" "SomeClass.java" 123
    {:java true :class "some.space.SomeClass$SomeInner" :method "someMethod" :file "SomeClass.java" :line 123}]

   ["some.space.SomeClass" "someMethod" nil -1
    {:java true :class "some.space.SomeClass" :method "someMethod" :file nil :line nil}]])

(def cases
  (mash
    (fn [[c m f l p]] [(StackTraceElement. c m f l) p])
    cases-data))

(deftest "parse-trace-elem"
  (doseq [[elem parsed] cases]
    (assert= parsed (parse-trace-elem elem))))

(deftest "parse-trace-elems"
  (assert= (map second cases) (parse-trace-elems (map first cases))))

(deftest "trim-redundant"
  (let [trim-fn (resolve 'clj-backtrace.core/trim-redundant)]
    (assert= '(d c) (trim-fn '(d c b a) '(f e b a)))
    (assert= '(c)   (trim-fn '(c b a)   '(f e b a)))
    (assert= '(d c) (trim-fn '(d c b a) '(e b a)))))

(deftest "parse-exception"
  (try
    (eval '(/))
    (catch Exception e (parse-exception e))))
