(ns cljs.user
  (:require [figwheel.client :as figwheel]))

(def foo 10)

(figwheel/start {:websocket-url "ws://localhost:3449/figwheel-ws"})

(set! (.-innerText (.getElementById js/document "main")) "Hello World")
