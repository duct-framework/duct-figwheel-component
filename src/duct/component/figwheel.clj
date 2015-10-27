(ns duct.component.figwheel
  "A component for running Figwheel servers."
  (:require [clojurescript-build.auto :as auto]
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

(defn cljs-repl
  "Open a ClojureScript REPL through the Figwheel server."
  ([{:keys [server builds]}]
   (assert (not (empty? builds)))
   (fig-repl/repl (first builds) server))
  ([{:keys [server builds]} build-id]
   (let [chosen-build (-> (group-by :id builds) (get build-id))]
     (assert (not (nil? chosen-build)))
     (fig-repl/repl chosen-build server))))

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
