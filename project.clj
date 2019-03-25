(defproject duct/figwheel-component "0.3.3"
  :description "A component for running Figwheel"
  :url "https://github.com/weavejester/duct-figwheel-component"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [suspendable "0.1.1"]
                 [figwheel-sidecar "0.5.14"]
                 [http-kit "2.2.0"]
                 [cider/piggieback "0.4.0"]]
  :profiles
  {:provided {:dependencies [[org.clojure/clojurescript "1.9.908"]]}
   :dev {:source-paths   ["dev/src/clj"]
         :resource-paths ["dev/resources" "target/js"]
         :dependencies [[reloaded.repl "0.2.3"]
                        [ring-jetty-component "0.3.1"]
                        [compojure "1.5.1"]
                        [figwheel "0.5.14"]
                        [nrepl "0.6.0"]]
         :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
