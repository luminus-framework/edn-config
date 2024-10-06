(ns config.core-test
  (:require [clojure.test :refer :all]
            [config.core :as e]))

(deftest test-env
  (testing "env variables"
    (is (= (System/getenv "USER") (:user (e/load-env))))
    (is (= (System/getenv "JAVA_ARCH") (:java-arch (e/load-env)))))
  (testing "system properties"
    (is (= (System/getProperty "user.name") (:user-name (e/load-env))))
    (is (= (System/getProperty "user.country") (:user-country (e/load-env)))))
  (testing "env file"
    (spit "test/config.edn" (prn-str {:foo "bar"}))
    (is (= "bar" (:foo (e/load-env)))))
  (testing "custom config"
    (is (= "custom prop" (:custom-prop (e/load-env {:custom-prop "custom prop"}))))))

(deftest number-test
  (System/setProperty "bignum" "112473406068224456955")
  (is (= 112473406068224456955 (:bignum (e/load-env)))))

(deftest edn-test
  (let [props {"BOOL"                "true"
               "text"                "\"true\""
               "db__spec"            "{:conn \"foo\"}"
               "number"              "15"
               "quoted-number"       "\"12\""
               "unparsed.text"       "some text here"
               "edn_string"          "{:foo :bar :baz [1 2 \"foo\"]}"
               "SOME__MARKED_QMARK_" "false"
               "WITH_BANG_"          ":bang!"
               "WITH_PLUS_"          ":plus+"
               "WITH_GT_"            ":gt>"
               "WITH_LT_"            ":lt<"
               "WITH_EQ_"            ":eq="
               "WITH_STAR_"          ":star*"}]
    (doseq [[k v] props] (System/setProperty k v))
    (is
      (= {:bool          true
          :text          "true"
          :number        15
          :quoted-number "12"
          :db/spec       {:conn "foo"}
          :edn-string    {:foo :bar, :baz [1 2 "foo"]}
          :unparsed-text "some text here"
          :some/marked?  false
          :with!         :bang!
          :with+         :plus+
          :with>         :gt>
          :with<         :lt<
          :with=         :eq=
          :with*         :star*}
         (select-keys (e/load-env)
                      [:bool
                       :text
                       :number
                       :quoted-number
                       :db/spec
                       :edn-string
                       :unparsed-text
                       :some/marked?
                       :with!
                       :with+
                       :with>
                       :with<
                       :with=
                       :with*])))))
