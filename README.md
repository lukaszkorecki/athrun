# athrun
[![Clojars Project](https://img.shields.io/clojars/v/athrun.svg)](https://clojars.org/athrun)

Athrun Zala, pilot of ZGMF-X19A ∞ Justice Gundam.

> Pun explaination: athRuns forever (infinite)

Simple scheduler [Component](https://github.com/stuartsierra/component) based on `j.u.c.ScheduledThreadPoolExecutor`. Supports dependency injection, multiple scheduled tasks and second resolution for repeated execution.

It's a spiritual success to [nomnom/eternity](https://github.com/nomnom-insights/nomnom.eternity)

## Installation


```clj
[athrun "0.0.1"]
```

## Usage

Scheduler is represented as a singular component, which manages a set of tasks. Tasks are wrapped by the component and receive its dependencies. Obvious limitation is that all tasks share dependencies of the schedueler component. In practice - it's fine, usually you don't want to do much work in the repeated task, and instead just queue up a job for background processing.

Simple example:

```

(def system
  {:scheduler (component/using
                 (athrun.core/create {:name "scheduler"
                                      :scheduled-tasks [
                                                        {:name "inactive-accounts"
                                                         ;; how often run this function?
                                                         :interval-seconds (* 15 60)
                                                         :handler (fn [{:keys [publisher]}]
                                                                    (publisher :check-accounts))}

                                                        {:name "email-queue"
                                                         :interval-seconds 15
                                                         ;; dealy the first run 30s after
                                                         ;; the component starts
                                                         :delay-seconds 30
                                                         :handler (fn [{:keys [publisher]}]
                                                                    (publisher :send-emails))}]})
                 [:publisher]) ;; some component for pushing messages to a background job queue
   })



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
