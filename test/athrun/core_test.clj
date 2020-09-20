(ns athrun.core-test
  (:require
    [athrun.core :as scheduler]
    [clojure.test :refer [deftest is]]
    [com.stuartsierra.component :as component]))


(def tasks
  [{:name "fast"
    :dependencies [:foo :state]
    :interval-seconds 1
    :handler (fn [{:keys [foo bar state]}]
               (assert foo)
               (assert (nil? bar))
               (swap! state #(update % :fast inc)))}

   {:name "slow"
    :dependencies [:state :bar]
    :interval-seconds 2
    :delay-seconds 2
    :handler (fn [{:keys [foo bar state]}]
               (assert bar)
               (assert (nil? foo))
               (swap! state #(update % :slow inc)))}])


(deftest just-running-around
  (let [state  (atom {:fast 0 :slow 0})
        system  (component/map->SystemMap
                  {:state state
                   :foo ::foo
                   :bar ::bar
                   :scheduler (component/using
                                (scheduler/create {:name "test"
                                                   :scheduled-tasks tasks})
                                [:state :foo :bar])})]
    (is (= {:fast 0 :slow 0} @state))
    (let [running (component/start system)]
      (is (true? (:running (:scheduler running))))
      (Thread/sleep 6050)
      (is (false? (:running (:scheduler (component/stop running)))))
      (let [{:keys [fast slow]} @state]
        (is (<= 6 fast 7))
        (is (<= 2 slow 3))))))
