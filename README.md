## `yogthos/config`

A library for managing configuration using environment variables and EDN configuration files.

The configuration is resolved in the following order, the variables found in later configurations will replace those declared earlier:

1. `config.edn` on the classpath
2. `.lein-env` file in the project directory
3. `.boot-env` file in the project directory
4. EDN file specified using the `config` environment variable
5. Environment variables
6. Java System properties

The library parses configuration keys into Clojure keywords with names lowercased, then `_` and `.` characters converted to dashes, e.g:

* `foo_bar` -> `foo-bar`
* `Foo_bar` -> `foo-bar`
* `Foo.BAR` -> `foo-bar`

namespaced keys can be declared using `__`:

* `db__spec` -> `db/spec`

The library uses ClojureScript-like substitution for special signs:

* `FOO_QMARK_` -> `foo?`
* `FOO_BANG_` -> `foo!`
* `FOO_PLUS_` -> `foo+`
* `FOO_GT_` -> `foo>`
* `FOO_LT_` -> `foo<`
* `FOO_EQ_` -> `foo=`
* `FOO_STAR_` -> `foo*`


The values are parsed using the following strategy:

1. `[0-9]+` -> number
2. `^(true|false)$` -> boolean
3. `\w+` -> string
4. try parse as EDN, and return the original value as the default

following environment variables:

```bash
* BOOL=true
* text="true"
* number=15
* quoted-number="12"
* db__spec="jdbc:sqlite:myapp_dev.db"
* edn_string="{:foo :bar :baz [1 2 \"foo\"]}"
* unparsed.text="some text here"
* WITH_BANG_=":bang!"
* WITH_PLUS_=":plus+"
```

are translated as:

``` clojure
* :bool          true,
* :text          "true",
* :number        15,
* :quoted-number "12",
* :db/spec       "jdbc:sqlite:myapp_dev.db"
* :edn-string    {:foo :bar, :baz [1 2 "foo"]},
* :unparsed-text "some text here"
* :with!         :bang!
* :with+         :plus+
```
## Installation

Include the following dependency in your `project.clj` file:

[![Clojars Project](https://img.shields.io/clojars/v/yogthos/config.svg)](https://clojars.org/yogthos/config)

## Usage

### external configuration

In most cases you'll likely want to specify the configuration file using the `config` environment variable.
We will add the dependency to our `project.clj` file and specify the configuration file location using `:jvm-opts`:

```clojure
(defproject edn-config-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [yogthos/config <VERSION>]]
  ;; configuration will be read from the dev-config.edn file               
  :jvm-opts ["-Dconfig=dev-config.edn"]               
  :main edn-config-test.core)

```

### embedded configuration

In some cases you may wish to package configuration in the jar along with the application. In this case the `config.edn` file must be present on the classpath.

Let's take a look at setting up separate configurations for development and production by adding profiles to `project.clj`. We'll create `dev` and `prod` profiles each pointing to a different resource path containing the desired configuration.

First, we'll need to create a `config` folder in the root of the project. Under the `config` we will create `dev`
and `prod` folders. Each of this will contain a file called `config.edn`.

We'll create a development configuration in `config/dev/config.edn`:

```clojure
{:db "jdbc:sqlite:dev.db"}
```

and a production configuration in `config/prod/config.edn`:

```clojure
{:db "jdbc:sqlite:prod.db"}
```

Next, we will add the dependency and the profiles to our `project.clj`:

```clojure
(defproject edn-config-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [yogthos/config <VERSION>]]
  :profiles {:prod {:resource-paths ["config/prod"]}
             :dev  {:resource-paths ["config/dev"]}}
  :main edn-config-test.core)

```

## Accessing the configuration

We can now access the config variables the `config.edn` found under the resource path specified in the profile.
There are two ways of doing this. We can load a version of config defined as `config.core/env`:

```clojure
(ns edn-config-test.core
  (:require [config.core :refer [env]])
  (:gen-class))

(defn -main []
  (println (:db env)))
```

The config in `env` can be reloaded at runtime by calling the `reload-env` function.

Alternatively, we can call the `config.core/load-env` explicitly to manage the state of the config in the app.
For example, if we use the [mount](https://github.com/tolitius/mount) library, we could write the following:

```Clojure
(ns edn-config-test.core
  (:require [mount.core :refer [defstate]]
            [config.core :refer [load-env]])
  (:gen-class))

(defstate env
  :start (load-env))

  (defn -main []
    (mount.core/start)
    (println (:db env)))    
```

## Packaging for release

The application can be packaged using a specific profile by using the Leiningen `with-profile` option. For example, if we wanted to package with the `prod` profile then we'd run the following:

```
lein with-profile prod uberjar
```

The resulting `jar` will contain the config found in `config/prod/config.edn` in case an embedded configuration was used:

```
java -jar target/edn-config-test.jar
=> jdbc:sqlite:prod.db
```


Alternatively, we can create a file called `custom-config.edn` that looks as follows:


```clojure
{:db "jdbc:sqlite:prod-custom.db"}
```

Then we can start the app and pass it the `config` environment variable pointing to the location of the file:

```
java -Dconfig="custom-config.edn" -jar target/edn-config-test.jar
=> jdbc:sqlite:prod-custom.db
```

### Attributions

The `yogthos/config` project is based on the [environ](https://github.com/weavejester/environ) library.

## License

Distributed under the Eclipse Public License, the same as Clojure.
