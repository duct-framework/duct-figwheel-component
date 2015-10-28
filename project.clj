(defproject duct/figwheel-component "0.2.0"
  :description "A component for running Figwheel"
  :url "https://github.com/weavejester/duct-figwheel-component"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]
                 [com.stuartsierra/component "0.3.0"]
                 [suspendable "0.1.0"]
                 [figwheel-sidecar "0.4.1"]
                 [clojurescript-build "0.1.9"]
                 [http-kit "2.1.19"]
                 [com.cemerick/piggieback "0.2.1"]]
  :profiles
  {:dev {:source-paths   ["dev/src/clj"]
         :resource-paths ["dev/resources" "target/js"]
         :dependencies [[reloaded.repl "0.2.1"]
                        [ring-jetty-component "0.3.0"]
                        [compojure "1.4.0"]
                        [figwheel "0.4.1"]
                        [org.clojure/tools.nrepl "0.2.10"]]
         :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
