(ns duct.component.figwheel
  (:require [clojurescript-build.auto :as auto]
            [com.stuartsierra.component :as component]
            [figwheel-sidecar.core :as fig-core]
            [figwheel-sidecar.auto-builder :as fig-auto]))

(defrecord Server [builds]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [server  (fig-core/start-server component)
            builder (auto/make-conditional-builder (fig-auto/builder server))
            state   (atom (mapv (comp builder auto/prep-build) builds))]
        (assoc component :server server, :builder builder, :state state))))
  (stop [component]
    (if-let [server (:server component)]
      (do (fig-core/stop-server server)
          (dissoc component :server :builder :state))
      component)))

(defn rebuild [{:keys [state builds builder]}]
  (reset! state (mapv (comp builder auto/prep-build) builds)))

(defn build [{:keys [state builder]}]
  (swap! state (partial mapv builder)))

(defn server [options]
  (map->Server options))
