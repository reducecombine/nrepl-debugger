(ns net.vemv.nrepl-debugger-test
  (:require
   [net.vemv.nrepl-debugger :as sut]
   [net.vemv.sample :as sample]
   [clojure.test :refer :all]))

(deftest accessible-forms
  (testing "The following can be accessed from a breakpoint being debugged: locals, vars, aliases, dynamic bindings"
    (let [context (sample/example 8)]
      (binding [sut/*eval-fn* context]
        (is (= [8 11 22 true]
               (eval '(sut/in-context [x ;; function argument
                                       y ;; let binding
                                       (omg y) ;; letfn binding
                                       sample/*doing-it?* ;; dynamic binding
                                       ]))))))))
