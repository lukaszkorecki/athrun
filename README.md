# athrun
[![Clojars Project](https://img.shields.io/clojars/v/athrun.svg)](https://clojars.org/athrun)

Athrun Zala, pilot of ZGMF-X19A ∞ Justice Gundam.

> Pun explaination: athRuns forever (infinite)

Simple scheduler [Component](https://github.com/stuartsierra/component) based on `j.u.c.ScheduledThreadPoolExecutor`. Supports dependency injection, multiple scheduled tasks and second resolution for repeated execution.

It's a spiritual successor to [nomnom/eternity](https://github.com/nomnom-insights/nomnom.eternity)

## Installation


```clj
[athrun "0.0.1"]
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
- interval-seconds - how often run the function
- delay-seconds - do not start the execution immediatedly, but wait X seconds before running the function for the first time. You can use this setting to model cron-like experssions when combined with `interval-seconds` (see below)
- name - name for logging

Simple example:

```clojure
(def tasks
  [{:name "inactive-accounts"
    ;; what does this task require?
    :dependencies [:publisher :redis]
    ;; how often run this function?
    :interval-seconds (* 15 60)
    :handler (fn [{:keys [publisher redis]}]
               (publisher :check-accounts (get-data-from redis)))}
   {:name "email-queue"
    :interval-seconds 15
    ;; note that dependencies are pulled out of the scheduler component
    :dependencies [:publisher :db-conn]
    ;; dealy the first run 30s after
    ;; the component starts
    :delay-seconds 30
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

- `athrun.middleware/with-lock` - based on [LockJaw](https://github.com/nomnom-insights/nomnom.lockjaw/)

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

- [ ] nail down the API
- [ ] more middlewares?
- [ ] better docs

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
