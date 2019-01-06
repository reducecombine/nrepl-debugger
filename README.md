# nrepl-debugger

A simple debugger that works by stopping the current thread, and launching a nREPL-enabled repl over a random port.

You can access said repl from a secondary terminal (or presumably IDE, but I haven't bothered trying).

> For your convenience, the connection command is copied to the clipboard. 

Once you connect, you have **full access** to the debugee's environment, including function arguments,
`let` and dynamic bindings, top-level vars, and aliases brought in via `require`.

As you exit, the stopped thread resumes execution.

## Rationale

I wanted a small, comprehensible debugger that I can trust to work in a variety of environments, agnostic of editor or concurrency model.

For example I may want to plant a `(debugger)` statement inside my test suite, which will be run in parallel with [eftest](https://github.com/weavejester/eftest). And I want to connect to that debugger from anywhere, free from intricacies or limitations of my debugger or IDE of choice.

This of course comes at the cost of feature-richness. GUIs, step-debugging or [frame exploration](https://github.com/pry/pry-stack_explorer) are awesome tools, but certainly ones which would compromise the original goals. 

## Usage

You can insert calls to `(debugger)` anywhere in your code:

```clojure

(def ^:dynamic
  *doing-it?*
  false)

(defn example [x]
  (binding [*doing-it?* true]
    (let [y (+ x 3)]
      (letfn [(omg [z]
                (* y 2))]
        (net.vemv.nrepl-debugger/debugger) ;; We inserted a debugging statement here
        3))))
```

Now, run `(example 98)` to trigger the debugger. It will print:

```
Entered debugger.
Started debugging server at port 54102. Copied connection command to the clipboard.
```

So, go over a terminal, and paste the connection command, which is simply `lein repl :connect 54102`.

You will see a plain repl in the `user` namespace. Regardless, all your code is loaded, the being-debugged environment is fully-accessible via the `in-context`  macro:

```clojure
user=> (net.vemv.nrepl-debugger/in-context [y *doing-it?*])
[101 true]
```

You can evaluate arbitrary forms inside `in-context`.

As you finish your repl session with <kbd>Control</kbd><kbd>D</kbd>, execution in your app will resume, cleanly stopping the nREPL server.

## Usage within threading macros

`debugger` ignores any passed arguments, making it apt for `doto`:

```clojure
(doto something debugger foo bar)
```

What about threading macros? The original `debugger` would break the underlying `->` because of its return value.

For that scenario, `debugger->` is provided:

```clojure
(-> something inc debugger-> dec)
````

`debugger->` works like `debugger`, except that it accepts one argument, exposing it as the `*debugger->*` dynamic variable,
and returning the argument so that `->` doesn't break.

That way, you can debug threading macros - an old problem!

There's no `debugger->>` - `debugger->` is enough for `->>`:

```clojure
(->> something inc debugger-> dec)
```

## Limitations

* For now a single global debugging-context and nREPL server are assumed, so don't try debugging things in parallel or with reentrancy.
* JVM-only.

## Snippet

Probably your IDE has some sort of configurable snippet system. As an idea, you can bind the following to `dbg`:

```clojure
(do (require 'net.vemv.nrepl-debugger) (net.vemv.nrepl-debugger/debugger))
```
