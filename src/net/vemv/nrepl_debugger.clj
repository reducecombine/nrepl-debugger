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

(def ^:dynamic
  *perform-side-effects?*
  "Whether `#'impl` should launch and nREPL and wait for it. Enables unit testing."
  true)

(defn impl []
  `(let [eval-fn# (eval-with-locals (local-bindings) *ns*)]
     (if-not *perform-side-effects?*
       eval-fn#
       (let [_# (println "Entered debugger.")
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
         (println "Exited debugger.")))))

(defmacro debugger
  "Opens a debugger repl over a nREPL server at a random port.

  Blocks the invoking thread, until a nREPL client gets connected AND finished its session by disconnecting.

  `#'debugger` disregards its arguments, which is handy for placing it in the middle of any expression
  (e.g. a large `doto` chain)."
  [& _]
  (impl))

(def ^:dynamic
  *debugger->*
  "See `#'debugger->`.")

(defmacro debugger->
  "Like `#'debugger`, but it returns its argument.

  Apt for being inserted in the middle of a `(-> ...)` or `(->> ...)` chain,
  without breaking the chain itself after the debugging session completes.

  The argument will be bound in the debugging environment as`*debugger->*`"
  [x]
  `(let [v# ~x]
     (binding [*debugger->* v#]
       ~(impl)
       v#)))
