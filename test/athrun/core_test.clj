(ns athrun.core-test
  (:require
    [clojure.test :refer [deftest is]]
    [athrun.core :as scheduler]
    [com.stuartsierra.component :as component]))


(def tasks
  [{:name "fast"
    :interval-seconds 1
    :handler (fn [{:keys [state]}]
               (swap! state #(update % :fast inc)))}

   {:name "slow"
    :interval-seconds 2
    :delay-seconds 2
    :handler (fn [{:keys [state]}]
               (swap! state #(update % :slow inc)))}])


(deftest just-running-around
  (let [state  (atom {:fast 0 :slow 0})
        system  (component/map->SystemMap
                  {:state state
                   :scheduler (component/using
                                (scheduler/create {:name "test"
                                                   :scheduled-tasks tasks})
                                [:state])})]
    (is (= {:fast 0 :slow 0} @state))
    (let [running (component/start system)]
      (is (true? (:running (:scheduler running))))
      (Thread/sleep 6050)
      (is (false? (:running (:scheduler (component/stop running)))))
      (let [{:keys [fast slow]} @state]
        (is (<= 6 fast 7))
        (is (<= 2 slow 3))))))
