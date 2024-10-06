(ns config.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as s])
  (:import java.io.PushbackReader))

(defn parse-number [^String v]
  (try
    (Long/parseLong v)
    (catch NumberFormatException _
      (BigInteger. v))))

;originally found in cprop https://github.com/tolitius/cprop/blob/6963f8e04fd093744555f990c93747e0e5889395/src/cprop/source.cljc#L26
(defn str->value
  "ENV vars and system properties are strings. str->value will convert:
   the numbers to longs, the alphanumeric values to strings, and will use Clojure reader for the rest
   in case reader can't read OR it reads a symbol, the value will be returned as is (a string)"
  [v]
  (cond
    (re-matches #"[0-9]+" v) (parse-number v)
    (re-matches #"^(true|false)$" v) (Boolean/parseBoolean v)
    (re-matches #"\w+" v) v
    :else
    (try
      (let [parsed (edn/read-string v)]
        (if (symbol? parsed) v parsed))
      (catch Throwable _ v))))

(defn keywordize [s]
  (-> s
      (s/replace "_QMARK_" "?")
      (s/replace "_BANG_" "!")
      (s/replace "_PLUS_" "+")
      (s/replace "_GT_" ">")
      (s/replace "_LT_" "<")
      (s/replace "_EQ_" "=")
      (s/replace "_STAR_" "*")
      (s/lower-case)
      (s/replace "__" "/")
      (s/replace "_" "-")
      (s/replace "." "-")
      (keyword)))

(defn read-system-env []
  (->> (System/getenv)
       (map (fn [[k v]] [(keywordize k) (str->value v)]))
       (into {})))

(defn read-system-props []
  (->> (System/getProperties)
       (map (fn [[k v]] [(keywordize k) (str->value v)]))
       (into {})))

(defn read-env-file [f & required]
  (when-let [env-file (io/file f)]
    (when (or (.exists env-file) required)
      (edn/read-string (slurp env-file)))))

(defn read-config-file [f]
  (try
    (when-let [url (or (io/resource f) (io/file f))]
      (with-open [r (-> url io/reader PushbackReader.)]
        (edn/read r)))
    (catch java.io.FileNotFoundException _)))

(defn contains-in?
  "checks whether the nested key exists in a map"
  [m k-path]
  (let [one-before (get-in m (drop-last k-path))]
    (when (map? one-before)                                 ;; in case k-path is "longer" than a map: {:a {:b {:c 42}}} => [:a :b :c :d]
      (contains? one-before (last k-path)))))

;; author of "deep-merge-with" is Chris Houser: https://github.com/clojure/clojure-contrib/commit/19613025d233b5f445b1dd3460c4128f39218741
(defn deep-merge-with
  "Like merge-with, but merges maps recursively, appling the given fn
  only when there's a non-map at a particular level.
  (deepmerge + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
               {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  -> {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    (remove nil? maps)))

(defn merge-maps [& m]
  (reduce #(deep-merge-with (fn [_ v] v) %1 %2) m))

(defn load-env
  "Generate a map of environment variables."
  [& configs]
  (let [env-props (merge-maps (read-system-env) (read-system-props))]
    (apply
      merge-maps
      (read-config-file "config.edn")
      (read-env-file ".lein-env")
      (read-env-file (io/resource ".boot-env"))
      (when (:config env-props)
        (read-env-file (:config env-props) true))
      env-props
      configs)))

(defonce
  ^{:doc "A map of environment variables."}
  env (load-env))

(defn reload-env []
  (alter-var-root #'env (fn [_] (load-env))))
