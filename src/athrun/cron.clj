(ns athrun.cron
  (:require
    [athrun.time :as time])
  (:import
    (com.cronutils.descriptor
      CronDescriptor)
    (com.cronutils.model
      Cron
      CronType)
    (com.cronutils.model.definition
      CronDefinition
      CronDefinitionBuilder)
    (com.cronutils.model.time
      ExecutionTime)
    (com.cronutils.parser
      CronParser)
    (java.time
      Duration
      Instant)
    (java.util
      Locale
      Optional)))


;; NOTE: we're using Quartz flavor of Cron to allow for second schedules
;; UNIX cron only supports minute schedules
(def parser
  (let [cronDefinition  (CronDefinitionBuilder/instanceDefinitionFor CronType/QUARTZ)]
    (CronParser. ^CronDefinition  cronDefinition)))


(def descriptor
  (CronDescriptor/instance (Locale/US)))


(defn parse [expr]
  (.parse ^CronParser parser expr))


(defn explain [^Cron parsed]
  (.describe ^CronDescriptor descriptor parsed))


(defn matches-schedule? [^Instant now ^Cron parsed]
  (let [now-in-utc (time/in-tz (time/->date-time now) time/UTC)
        execution-time (ExecutionTime/forCron parsed)]
    (.isMatch ^ExecutionTime execution-time now-in-utc)))


(defn next-execution [^Instant now ^Cron parsed]
  (let [now-in-utc (time/in-tz (time/->date-time now) time/UTC)
        execution-time (ExecutionTime/forCron parsed)
        next (.get ^Optional (.nextExecution ^ExecutionTime execution-time now-in-utc))]
    (time/->instant next)))


(defn next-execution-in-seconds [^Instant now ^Cron parsed]
  (let [now-in-utc (time/in-tz (time/->date-time now) time/UTC)
        execution-time (ExecutionTime/forCron parsed)
        next (.get ^Optional (.timeToNextExecution ^ExecutionTime execution-time now-in-utc))
        is-now-matching? (.isMatch ^ExecutionTime execution-time now-in-utc)]
    (if is-now-matching?
      ;; NOTE: if current time matches the cron expression -
      ;; ExecutionTime/next will return the NEXT execution rather than current
      ;; kinda makes sense, but we need to have stable cron execution!
      0
      (.getSeconds ^Duration next))))


(defn execution-interval-in-seconds [^Instant now ^Cron parsed]
  (let [now-in-utc (time/in-tz (time/->date-time now) time/UTC)
        ^ExecutionTime execution-time (ExecutionTime/forCron parsed)

        ;; NOTE:
        ;; We could calculate duration by checking *previous* and *next* execution relative
        ;; to current time in UTC, but instead use next execution and one after. Why?
        ;; because cronutils has a bug: if current time happens to be on 0th second previous execution will be truncated
        ;; to not include seconds and that throws off java.time.Duration/between and it reports that diff is 1s
        ;;
        next (.get ^Optional (.nextExecution execution-time now-in-utc))
        next-next (.get ^Optional (.nextExecution execution-time next))

        duration (Duration/between ^Instant (time/->instant next)
                                   ^Instant (time/->instant next-next))
        duration-s (.getSeconds ^Duration duration)]
    (if (zero? duration-s)
      (throw (ex-info "somehow duration is 0!" {:now now
                                                :next-previous next-next
                                                :next next
                                                :duration duration
                                                :now-in-utc now-in-utc
                                                :explained (explain parsed)
                                                :schedule (.asString parsed)}))
      duration-s)))
