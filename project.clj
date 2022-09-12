(defproject athrun "0.0.3-SNAPSHOT"
  :description "Scheduler component, based on ScheduledExecutor, with CRON expression support"
  :url "https://github.com/lukaszkorecki/athrun"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.cronutils/cron-utils "9.2.0"]
                 [tick/tick  "0.5.0"
                  :exclusions [cljs.java-time/cljs.java-time
                               cljsjs/js-joda-locale-en-us
                               cljsjs/js-joda-timezone]]]
  :deploy-repositories [["releases" :clojars]]
  :jvm-opts ^:replace ["-XX:-OmitStackTraceInFastThrow"
                       "-Duser.timezone=UTC"
                       "-Dfile.encoding=UTF-8"]
  :profiles {:dev {:dependencies [[nomnom/lockjaw "0.3.1"]
                                  [org.slf4j/slf4j-api "2.0.0"]
                                  [org.clojure/tools.logging "1.2.4"]
                                  [ch.qos.logback/logback-classic "1.4.0"
                                   :exclusions [org.slf4j/slf4j-api]]
                                  [com.stuartsierra/component "1.1.0"]]}})
