(ns net.vemv.nrepl-debugger-test
  (:require
   [net.vemv.nrepl-debugger :as sut]
   [net.vemv.sample :as sample]
   [clojure.test :refer :all]))

(deftest accessible-forms
  ;; NOTE: aliases can't be tested. e.g. I wanted to include `debugger/something`. It works over nREPL but not here.
  ;; That's also the reason why `net.vemv.sample/*doing-it?*` is not simply `*doing-it?*`.
  (testing "The following can be accessed from a breakpoint being debugged: locals, vars, dynamic bindings"
    (binding [sut/*perform-side-effects?* false]
      (let [context (sample/example 8)]
        (binding [sut/*eval-fn* context]
          (is (= [8 11 22 true]
                 (eval '(net.vemv.nrepl-debugger/in-context [x ;; function argument
                                                             y ;; let binding
                                                             (omg y) ;; letfn binding
                                                             net.vemv.sample/*doing-it?* ;; dynamic binding
                                                             ])))))))))

(deftest debugger->
  (testing "It doesn't break the surrounding thread forms"
    (binding [sut/*perform-side-effects?* false]
      (is (zero? (-> 1 sut/debugger-> dec)))
      (is (zero? (->> 1 sut/debugger-> dec))))))
