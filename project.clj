(defproject athrun "0.0.2-SNAPSHOT"
  :description "Scheduler component, based on ScheduledExecutor"
  :url "https://github.com/lukaszkorecki/athrun"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[nomnom/lockjaw "0.2.0-SNAPSHOT"]
                                  [org.slf4j/slf4j-api "1.7.30"]
                                  [org.clojure/tools.logging "1.1.0"]
                                  [ch.qos.logback/logback-classic "1.2.3"
                                   :exclusions [org.slf4j/slf4j-api]]
                                  [com.stuartsierra/component "1.0.0"]]}})
