# Duct-Figwheel-Component

A [component][] for the [Figwheel][] development tool, designed to be
used in the [Duct][] framework (but can be used in any component-based
system).

[component]: https://github.com/stuartsierra/component
[figwheel]:  https://github.com/bhauman/lein-figwheel
[duct]:      https://github.com/weavejester/duct

## Installation

Add the following dependency to your `project.clj`:

    [duct/figwheel-component "0.3.4"]

## Usage

Require the library and the Component library:

```clojure
(require '[duct.component.figwheel :as figwheel]
         '[com.stuartsierra.component :as component])
```

Setup the component with a Figwheel-compatible configuration:

```clojure
(def figwheel
  (figwheel/server
   {:css-dirs ["resources/public/css"]
    :builds
    [{:source-paths ["src/cljs"]
      :build-options
      {:output-to "target/figwheel/public/main.js"
       :output-dir "target/figwheel/public"
       :optimizations :none}}]}))
```

Start the server:

```clojure
(alter-var-root #'figwheel component/start)
```

Ensure that the client ClojureScript starts Figwheel as well:

```clojure
(figwheel.client/start {:websocket-url "ws://localhost:3449/figwheel-ws"})
```

Unlike the Leiningen plugin of Figwheel, this component does not
trigger builds automatically. Instead, you have the following
functions at the REPL:

```clojure
(figwheel/build-cljs figwheel)    ;; build modified cljs files
(figwheel/rebuild-cljs figwheel)  ;; build all cljs files
(figwheel/refresh-css figwheel)   ;; refresh CSS files
```

Running any of those functions will cause the updated code to be
pushed over websocket to any open clients.

You can also start a Piggieback REPL over the Figwheel connection:

```clojure
(figwheel/cljs-repl figwheel)           ;; uses first build
(figwheel/cljs-repl figwheel build-id)  ;; uses specific build-id
```

This REPL will allow you to evaluate ClojureScript on the browser.

To stop the server:

```clojure
(alter-var-root #'figwheel component/stop)
```

## License

Copyright © 2019 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
