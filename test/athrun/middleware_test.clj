(ns athrun.middleware-test
  (:require
    [athrun.core :as scheduler]
    [athrun.middleware :as scheduler.middleware]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.stuartsierra.component :as component]
    [lockjaw.mock :as lockjaw]))


(def execution-state  (atom nil))

(def system (atom nil))


(def tasks
  [{:name "every-3-seconds"
    :dependencies [:foo]
    :schedule "/3 * * * * ?"
    :handler (scheduler.middleware/with-lock (fn [_]
                                               (swap! execution-state #(update % :every-3-seconds inc))))}
   {:name "every-5-seconds"
    :dependencies [:bar]
    :schedule "/5 * * * * ?"
    :handler (scheduler.middleware/with-lock (fn [_]
                                               (swap! execution-state #(update % :every-5-seconds inc))))}])


(use-fixtures :once (fn [test-fn]
                      (reset! execution-state {:every-3-seconds 0 :every-5-seconds 0})
                      (reset! system (component/start-system
                                       (component/map->SystemMap
                                         {:lock (lockjaw/create {:always-acquire false})
                                          :foo ::foo
                                          :bar ::bar
                                          :scheduler (component/using
                                                       (scheduler/create {:name "test"
                                                                          :scheduled-tasks tasks})
                                                       [:lock :foo :bar])})))
                      (test-fn)
                      (swap! system component/stop-system)))


(deftest lock-is-not-acquired
  (Thread/sleep (* 1000 6))
  (is (true? (:running (:scheduler @system))))
  (let [{:keys [every-3-seconds every-5-seconds]} @execution-state]
    (is (zero? every-5-seconds))
    (is (zero? every-3-seconds))))
