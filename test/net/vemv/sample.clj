(ns net.vemv.sample
  "Sample data for unit testing."
  (:require
   [net.vemv.nrepl-debugger :as debugger]))

(def ^:dynamic
  *doing-it?*
  false)

(defn example [x]
  (binding [*doing-it?* true]
    (let [y (+ x 3)]
      (letfn [(omg [z]
                (* y 2))]
        (debugger/eval-with-locals (debugger/local-bindings) *ns*)))))
