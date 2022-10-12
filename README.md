# athrun
[![Clojars Project](https://img.shields.io/clojars/v/athrun.svg)](https://clojars.org/athrun)

Athrun Zala, pilot of ZGMF-X19A ∞ Justice Gundam.

> Pun explaination: athRuns forever (infinite)

Simple scheduler [Component](https://github.com/stuartsierra/component) based on `j.u.c.ScheduledThreadPoolExecutor`. Supports dependency injection, multiple scheduled tasks and second resolution for repeated execution. Best of all, you can use cron expressions to configure when your handlers will execute.

It's a spiritual successor to [nomnom/eternity](https://github.com/nomnom-insights/nomnom.eternity) and very simplified version of QuartzScheduler.

## Installation


```clj
[athrun "0.0.3-SNAPSHOT"]
```

## Usage

Scheduler is represented as a singular component, which manages a set of tasks. Tasks are wrapped by the component and receive the subset of the scheduler component dependencies.


### Configuration

Scheduler component:

- name (just for logging)
- tasks

Tasks:

Each task requires the following:

- handler - the scheduled function, receives a map of components it depends on
- dependencies - **vector of keywords** to select from the scheduler component's deps
- schedule - a Cron expression, with Quartz Scheduler extension allowing for specifying schedules down to a second (UNIX Cron can only schedule up to a minute)
- name - name for logging

Simple example:

```clojure
(def tasks
  [{:name "inactive-accounts"
    ;; what does this task require?
    :dependencies [:publisher :redis]
    ;; run every 5m on 0th, 5th, 10th m of hour
    :schedule "0 /5  * * * ?"
    :handler (fn [{:keys [publisher redis]}]
               (publisher :check-accounts (get-data-from redis)))}
   {:name "email-queue"
    ;; 2am UTC
    :schedule "0 0 2 * * *"
    ;; note that dependencies are pulled out of the scheduler component
    :dependencies [:publisher :db-conn]
    :handler (fn [{:keys [publisher db-conn]}]
               (store-results db-conn (publisher :send-emails)))}])


(def system
  {:scheduler (component/using
               (athrun.core/create {:name "scheduler"
                                    :scheduled-tasks tasks})
               ;; some component for pushing messages to a background job queue, redis and db clients
               ;; the scheduler component has to depend on all dependencies of its tasks
               [:publisher :redis :db-conn])})

```

### Middlewares

Similarly to Eternity, middlewares are supported and VERY simple. There are two provided:

- `athrun.middleware/with-lock` - based on [LockJaw](https://github.com/nomnom-insights/nomnom.lockjaw/) require's `lock` component in the system and dependencies list

- `athrun.middleware/with-logging` - simple logger based middleware, with DEBUG logs


#### Usage

Define your task like so:

```

(def scheduled-handler
  (athrun.middleware/with-lock
    (athunr.middleware/wthi-logging
     (fn []
       (println "i'll do some work, but only if I have the lock")))))
```


and use it in your task definition.


## Roadmap

- [ ] better docs
- [ ] more testing

## License

Copyright © 2018 - 2022 Lukasz Korecki

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
