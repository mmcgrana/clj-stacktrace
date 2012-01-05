(ns clj-stacktrace.utils)

(defn rjust
  "If width is greater than the length of s, returns a new string
  of length width with s right justified within it, otherwise returns s."
  [width s]
  (format (str "%" width "s") s))

(defn quartile1
  "Compute the first quartile for the given collection according to
  Tukey (Hoaglin et al. 1983). coll must be sorted."
  ;; Hoaglin, D.; Mosteller, F.; and Tukey, J. (Ed.).
  ;; Understanding Robust and Exploratory Data Analysis.
  ;; New York: Wiley, pp. 39, 54, 62, 223, 1983.
  [coll]
  (let [c (count coll)]
    (nth coll (if (even? c)
                (/ (+ c 2) 4)
                (/ (+ c 3) 4)))))

(defn quartile3
  "Compute the third quartile for the given collection according to
  Tukey (Hoaglin et al. 1983). coll must be sorted."
  ;; Hoaglin, D.; Mosteller, F.; and Tukey, J. (Ed.).
  ;; Understanding Robust and Exploratory Data Analysis.
  ;; New York: Wiley, pp. 39, 54, 62, 223, 1983.
  [coll]
  (let [c (count coll)]
    (nth coll (if (even? c)
                (/ (+ (* 3 c) 2) 4)
                (/ (inc (* 3 c)) 4)))))

(defn fence
  "Compute the upper outer fence for the given coll. coll must be sorted."
  [coll]
  (let [q1  (quartile1 coll)
        q3  (quartile3 coll)
        iqr (- q3 q1)]
    (int (+ q3 (/ (* 3 iqr) 2)))))

(defn- omitter-fn [to-omit]
  (if (instance? java.util.regex.Pattern to-omit)
    ;; Curse you, non ifn regexes!
    (comp (partial re-find to-omit) pr-str)
    to-omit))

(defn omit-frames
  "Remove frames matching to-omit, which can be a function or regex."
  [trace-elems to-omit]
  (if-let [omit? (omitter-fn to-omit)]
    (reduce (fn [trace-elems elem]
              (if (omit? elem)
                trace-elems
               (conj trace-elems elem))) [] trace-elems)
    trace-elems))
