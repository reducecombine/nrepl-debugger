(ns net.vemv.nrepl-debugger
  (:require
   [clipboard.core :as clipboard]
   [nrepl.server :as nrepl]))

(def ^:dynamic
  *locals* nil)

(defn eval-with-locals [locals ns]
  (bound-fn* (fn [form]
               (binding [*locals* locals
                         *ns* ns]
                 (eval
                  `(let ~(vec (mapcat #(list % `(*locals* '~%))
                                      (keys locals)))
                     ~form))))))

(defmacro local-bindings []
  (let [symbols (keys &env)]
    (zipmap (map (fn [sym]
                   `(quote ~sym))
                 symbols)
            symbols)))

(def ^:dynamic
  *eval-fn*
  nil)

(def global-eval-fn
  nil)

(def global-nrepl-server
  nil)

(defmacro in-context
  "Evaluates all forms as if they were in the same exact context where `#'debugger` was invoked."
  [& forms]
  ((or *eval-fn* global-eval-fn) `(do ~@forms)))

(defn exit []
  (some-> global-nrepl-server nrepl/stop-server))

(defn running? [server]
  (some-> server :open-transports deref seq boolean))

(defmacro debugger
  "Opens a debugger repl over a nREPL server at a random port.

  Blocks the invoking thread, until a nREPL client gets connected AND finished its session by disconnecting.

  `#'debugger` disregards its arguments, which is handy for placing it in the middle of any expression
  (e.g. a large `doto` chain)."
  [& _]
  `(let [_# (println "Entered debugger.")
         eval-fn# (eval-with-locals (local-bindings) *ns*)
         _# (alter-var-root #'global-eval-fn (constantly eval-fn#))
         server# (nrepl/start-server)
         _# (println (str "Started debugging server at port " (:port server#) ". Copied connection command to the clipboard."))
         _# (clipboard/spit (str "lein repl :connect " (:port server#)))
         _# (alter-var-root #'global-nrepl-server (constantly server#))]
     (while (not (running? server#)) ;; wait for a connection...
       (Thread/yield))
     (while (running? server#) ;; wait for its disconnection...
       (Thread/yield))
     (exit)
     (println "Exited debugger.")))
