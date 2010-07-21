(ns clj-stacktrace.utils)

(defn mash
  "Reduce a seq-able to a map. The given fn should return a 2-element tuple
  representing a key and value in the new map."
  [f coll]
  (reduce
    (fn [memo elem]
      (let [[k v] (f elem)]
        (assoc memo k v)))
    {} coll))

(defn re-gsub
  "Simple version of re-gsub that only supports string replacements."
  [#^java.util.regex.Pattern regex replacement #^String string]
  (.. regex (matcher string) (replaceAll replacement)))

(defn re-without
  "Returns a String with the given pattern re-gsub'd out the given string."
  [pattern string]
  (re-gsub pattern "" string))

(defn re-match?
  "Returns true iff the given string contains a match for the given pattern."
  [#^java.util.regex.Pattern pattern string]
  (.find (.matcher pattern string)))

(defn re-get
  "Returns the nth captured group resulting from matching the given pattern
  against the given string, or nil if no match is found."
  [re s n]
  (let [m (re-matcher re s)]
    (if (.find m)
      (.group m n))))

(defn high
  "Like max, but for collections."
  [vals]
  (apply max vals))

(defn zip
  "Zip collections into tuples."
  [& colls]
  (apply map list colls))

(defn rjust
  "If width is greater than the length of s, returns a new string
  of length width with s right justified within it, otherwise returns s."
  [width s]
  (format (str "%" width "s") s))

(defn quartile1
  "Compute the first quartile for the given collection according to
  Tukey (Hoaglin et al. 1983). coll must be sorted."
  ; Hoaglin, D.; Mosteller, F.; and Tukey, J. (Ed.).
  ; Understanding Robust and Exploratory Data Analysis.
  ; New York: Wiley, pp. 39, 54, 62, 223, 1983.
  [coll]
  (let [c (count coll)]
    (nth coll (if (even? c)
                (/ (+ c 2) 4)
                (/ (+ c 3) 4)))))

(defn quartile3
  "Compute the third quartile for the given collection according to
  Tukey (Hoaglin et al. 1983). coll must be sorted."
  ; Hoaglin, D.; Mosteller, F.; and Tukey, J. (Ed.).
  ; Understanding Robust and Exploratory Data Analysis.
  ; New York: Wiley, pp. 39, 54, 62, 223, 1983.
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
