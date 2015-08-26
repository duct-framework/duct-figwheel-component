(defproject duct/figwheel-component "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48" :scope "provided"]
                 [com.stuartsierra/component "0.2.3"]
                 [figwheel-sidecar "0.3.7"]
                 [clojurescript-build "0.1.8"]
                 [http-kit "2.1.19"]]
  :profiles
  {:dev {:source-paths   ["dev/src/clj"]
         :resource-paths ["dev/resources" "target/js"]
         :dependencies [[reloaded.repl "0.1.0"]
                        [ring-jetty-component "0.2.3"]
                        [compojure "1.4.0"]
                        [figwheel "0.3.7"]]}})
