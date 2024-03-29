(ns athrun.core-test
  (:require
    [athrun.core :as scheduler]
    [clojure.test :refer [deftest is use-fixtures]]
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]))


(def execution-state  (atom nil))

(def system (atom nil))


(def tasks
  [{:name "every-3-seconds"
    :dependencies [:foo]
    :schedule "/3 * * * * ?"
    :handler (fn [{:keys [foo]}]
               (is (= ::foo foo))
               (log/info "every 3s?" @execution-state)
               (swap! execution-state #(update % :every-3-seconds inc)))}
   {:name "every-5-seconds"
    :dependencies [:bar]
    :schedule "/5 * * * * ?"
    :handler (fn [{:keys [bar]}]
               (is (= ::bar bar))

               (swap! execution-state #(update % :every-5-seconds inc)))}])


(use-fixtures :once (fn [test-fn]
                      (reset! execution-state {:every-3-seconds 0 :every-5-seconds 0})
                      (reset! system (component/start-system
                                       (component/map->SystemMap
                                         {:foo ::foo
                                          :bar ::bar
                                          :scheduler (component/using
                                                       (scheduler/create {:name "test"
                                                                          :scheduled-tasks tasks})
                                                       [:foo :bar])})))
                      (test-fn)
                      (swap! system component/stop-system)))


(deftest just-running-around
  (Thread/sleep (* 1000 20))
  (is (true? (:running (:scheduler @system))))
  ;; account for jitter and accept ranges
  (let [{:keys [every-3-seconds every-5-seconds]} @execution-state]
    (is (<= 5 every-3-seconds 7))
    (is (<= 3 every-5-seconds 5))))
