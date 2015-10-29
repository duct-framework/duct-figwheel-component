(ns duct.component.figwheel
  "A component for running Figwheel servers."
  (:require [cemerick.piggieback :as piggieback]
            [cljs.repl :as repl]
            [cljs.stacktrace :as stacktrace]
            [clojurescript-build.auto :as auto]
            [com.stuartsierra.component :as component]
            [compojure.core :as compojure :refer [GET]]
            [compojure.route :as route]
            [figwheel-sidecar.core :as fig-core]
            [figwheel-sidecar.auto-builder :as fig-auto]
            [figwheel-sidecar.repl :as fig-repl]
            [org.httpkit.server :as httpkit]
            [ring.middleware.cors :as cors]
            [suspendable.core :as suspendable]))

(defrecord FigwheelBuild [])
(defrecord FigwheelServer [])

(defmethod print-method FigwheelBuild [_ ^java.io.Writer writer]
  (.write writer "#<FigwheelBuild>"))

(defmethod print-method FigwheelServer [_ ^java.io.Writer writer]
  (.write writer "#<FigwheelServer>"))

(defn- figwheel-server [state]
  (-> (compojure/routes
       (GET "/figwheel-ws/:desired-build-id" [] (fig-core/reload-handler state))
       (GET "/figwheel-ws" [] (fig-core/reload-handler state))
       (route/not-found "<h1>Page not found</h1>"))
      (cors/wrap-cors
       :access-control-allow-origin #".*"
       :access-control-allow-methods [:head :options :get :put :post :delete :patch])
      (httpkit/run-server
       {:port      (:server-port state)
        :server-ip (:server-ip state "0.0.0.0")
        :worker-name-prefix "figwh-httpkit-"})))

(defn- start-figwheel-server [opts]
  (let [state  (fig-core/create-initial-state (fig-core/resolve-ring-handler opts))
        server (figwheel-server state)]
    (map->FigwheelServer (assoc state :http-server server))))

(defn- start-build [builder build]
  (-> build auto/prep-build builder map->FigwheelBuild))

(defn rebuild-cljs
  "Tell a Figwheel server component to rebuild all ClojureScript source files,
  and to send the new code to the connected clients."
  [{:keys [state builds builder]}]
  (reset! state (mapv (partial start-build builder) builds)) nil)

(defn build-cljs
  "Tell a Figwheel server component to build any modified ClojureScript source
  files, and to send the new code to the connected clients."
  [{:keys [state builder]}]
  (swap! state (partial mapv builder)) nil)

(defn refresh-css
  "Tell a Figwheel server component to update the CSS of connected clients."
  [{:keys [server]}]
  (fig-core/check-for-css-changes server) nil)

(defn- repl-print [& args]
  (apply (:print repl/*repl-opts* println) args))

(defn- add-repl-print-callback! [{:keys [browser-callbacks]}]
  (swap! browser-callbacks assoc "figwheel-repl-print" #(apply repl-print %)))

(defrecord FigwheelEnv [server]
  repl/IJavaScriptEnv
  (-setup [_ _]
    (add-repl-print-callback! server)
    (fig-repl/wait-for-connection server)
    (Thread/sleep 500))
  (-evaluate [_ _ _ js]
    (fig-repl/wait-for-connection server)
    (fig-repl/eval-js server js))
  (-load [_ _ url]
    (fig-repl/wait-for-connection server)
    (fig-repl/eval-js server (slurp url)))
  (-tear-down [_] true)
  repl/IParseStacktrace
  (-parse-stacktrace [repl-env _ error build-options]
    (stacktrace/parse-stacktrace
     (merge repl-env (fig-repl/extract-host-and-port (:base-path error)))
     (:stacktrace error)
     {:ua-product (:ua-product error)}
     build-options))
  repl/IPrintStacktrace
  (-print-stacktrace [_ stacktrace _ build-options]
    (doseq [{:keys [function file url line column] :as line-tr}
            (repl/mapped-stacktrace stacktrace build-options)
            :when (fig-repl/valid-stack-line? line-tr)]
      (repl-print "\t" (str function " (" (str (or url file)) ":" line ":" column ")")))))

(defn- figwheel-env [server build]
  (assoc (->FigwheelEnv server) :cljs.env/compiler (:compiler-env build)))

(defn- start-piggieback-repl [server build]
  {:pre [(some? build)]}
  (let [build    (auto/prep-build build)
        compiler (or (:compiler build) (:build-options build))]
    (piggieback/cljs-repl
     (figwheel-env server build)
     :special-fns  (:special-fns compiler repl/default-special-fns)
     :output-dir   (:output-dir compiler "out")
     :analyze-path (:source-paths build))))

(defn cljs-repl
  "Open a ClojureScript REPL through the Figwheel server."
  ([{:keys [server builds]}]
   (start-piggieback-repl server (first builds)))
  ([{:keys [server builds]} build-id]
   (start-piggieback-repl server (-> (group-by :id builds) (get build-id)))))

(defrecord Server [builds]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [server  (start-figwheel-server component)
            builder (auto/make-conditional-builder (fig-auto/builder server))
            state   (atom (mapv (partial start-build builder) builds))]
        (assoc component :server server, :builder builder, :state state))))
  (stop [component]
    (if-let [server (:server component)]
      (do (fig-core/stop-server server)
          (dissoc component :server :builder :state))
      component))
  suspendable/Suspendable
  (suspend [component] component)
  (resume [component old-component]
    (if (and (:server old-component) (= builds (:builds old-component)))
      (doto (into component (select-keys old-component [:server :builder :state]))
        (build-cljs)
        (refresh-css))
      (do (component/stop old-component)
          (component/start component)))))

(defn server
  "Create a new Figwheel server with the supplied option map. See the Figwheel
  documentation for a full explanation of what options are allowed."
  [options]
  (map->Server options))
