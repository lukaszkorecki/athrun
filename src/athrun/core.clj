(ns athrun.core
  (:require
    [athrun.cron :as cron]
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component])
  (:import
    (java.time
      Instant)
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
  [^ScheduledThreadPoolExecutor executor {:keys [name handler delay-seconds interval-seconds cron-schedule schedule start-at] :as _args}]
  (log/infof "create-task=%s delay=%s interval=%s schedule=%s (%s) start-at=%s\n"
             name
             delay-seconds
             interval-seconds
             (cron/explain cron-schedule)
             schedule
             start-at)
  (.scheduleWithFixedDelay executor
                           ^Runnable handler
                           (or delay-seconds 0)
                           interval-seconds
                           TimeUnit/SECONDS))


(defn wrap-handler [component {:keys [dependencies] :as task} handler]
  (let [wrapped (fn wrapped' []
                  (try
                    (if (seq dependencies)
                      (handler (select-keys component dependencies))
                      (handler (dissoc component
                                       :name :scheduled-tasks
                                       :task-pool :executor)))
                    (catch Throwable err
                      (log/errorf err "scheduled function error"))))]
    (with-meta wrapped (dissoc task :handler))))


(defrecord Scheduler [name scheduled-tasks
                      ;; internal state
                      task-pool
                      executor]
  component/Lifecycle
  (start [this]
    (let [executor (ScheduledThreadPoolExecutor. (count scheduled-tasks))
          task-pool (->> scheduled-tasks
                         ;; wrap scheduled handler invocations so that we pass
                         ;; the component instance, but with required dependencies only
                         (mapv (fn configure-tasks [{:keys [schedule] :as task}]
                                 (let [cron-schedule (cron/parse schedule)
                                       now (Instant/now)]
                                   (-> task
                                       (assoc :cron-schedule cron-schedule)
                                       (update :handler (partial wrap-handler this task))
                                       (assoc :start-at (cron/next-execution now cron-schedule))
                                       (assoc :interval-seconds (cron/execution-interval-in-seconds now cron-schedule))
                                       (assoc :delay-seconds (cron/next-execution-in-seconds now cron-schedule))))))
                         (mapv #(schedule-task executor %)))]
      (log/infof "scheduler=%s executor=%s starting tasks=%s" name executor (count task-pool))
      (assoc this :executor executor :task-pool task-pool :running true)))
  (stop [this]
    (log/warnf "scheduler=%s executor=%s stopping tasks=%s" name executor (count task-pool))
    (.shutdown  ^ScheduledThreadPoolExecutor executor)
    (assoc this :running false)))


(defn create
  "Create a scheduler along with task config.
  Notes:
  - all tasks will run at the 0th second of each minute, everything is guaranteed to run at a minute resolution
  - delay can be omitted, or specified as hh:mm expression using `start-at` option
  Options:
  - name - name of the scheduler pool, used for logging
  - scheduled-tasks - vector of maps, each map accepts:
    - name - name of the scheduled task
    - interval-seconds - how often to run the task
    - start-at - optional, expression of hh:mm or :mm  for running at a specific time of day or minutes past hour
    - dependencies - optional, vector of keywords representing subset of component dependencies of the scheduler,
                     if missing handler will receive of all scheduler pool dependencies
    - handler - single arity function receiving components that the scheduler pool depends on"
  [{:keys [name scheduled-tasks]}]
  {:pre [(string? name)
         (every? (fn [{:keys [handler name schedule dependencies]}]
                   (and
                     (fn? handler)
                     (string? schedule)
                     (or (string? name) (nil? name))
                     (or (nil? dependencies) (every? keyword? dependencies))))
                 scheduled-tasks)]}
  (map->Scheduler {:name name
                   :scheduled-tasks scheduled-tasks}))
