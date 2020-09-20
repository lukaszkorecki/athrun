(ns athrun.core
  (:require
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component])
  (:import
    (java.util.concurrent
      ScheduledThreadPoolExecutor
      TimeUnit)))


(defn schedule-task
  "Takes a scheduled task, map defined by
  - hnadler - a function
  - delay-sconds - run the handler after a delay
  - interval-seconds - run handler on a interval
  if delay-only passed: handler runs once
  if interval-only passed: handler runs immediatedly on a schedule
  if both delay+interval passed: handler runs with a delay, then fixed schedule"
  [^ScheduledThreadPoolExecutor executor {:keys [name handler delay-seconds interval-seconds start-at]}]
  (log/infof "create-task=%s delay=%s interval=%s start-at=%s" name delay-seconds interval-seconds start-at)
  (.scheduleWithFixedDelay executor
                           ^Runnable handler
                           (or delay-seconds 0)
                           interval-seconds
                           TimeUnit/SECONDS))


(defrecord Scheduler [name scheduled-tasks
                      ;; internal state
                      task-pool
                      executor]
  component/Lifecycle
  (start [this]
    (let [executor (ScheduledThreadPoolExecutor. 2)
          task-pool (->> scheduled-tasks
                         ;; wrap scheduled handler invocations so that we pass
                         ;; the component instance, but with required dependencies only
                         (mapv #(update % :handler (fn [original-handler]
                                                     (fn []
                                                       (try
                                                         (original-handler (select-keys this (:dependencies %)))
                                                         (catch Throwable err
                                                           (log/errorf err "scheduled function error")))))))
                         (mapv #(schedule-task executor %)))]
      (log/infof "scheduler=%s executor=%s starting tasks=%s" name executor (count task-pool))

      (assoc this :executor executor :task-pool task-pool :running true)))
  (stop [this]
    (log/warnf "scheduler=%s executor=%s stopping tasks=%s" name executor (count task-pool))
    (.shutdown  ^ScheduledThreadPoolExecutor executor)
    (assoc this :running false)))


(defn create [{:keys [name scheduled-tasks]}]
  {:pre [(string? name)
         (every? (fn [{:keys [handler name delay-seconds interval-seconds]}]
                   (and
                     (fn? handler)
                     (or (string? name) (nil? name))
                     (or (number? interval-seconds)
                         (number? delay-seconds)))) scheduled-tasks)]}
  (map->Scheduler {:name name
                   :scheduled-tasks scheduled-tasks}))
