(ns athrun.time
  (:require
    [tick.core :as t]))


;; XXX do we need tick at all?
;; FIXME - just use JDK stuff directly, it's not that complciated

(def now t/now)


(defn now-unix-time
  ([]
   (now-unix-time (now)))
  ([now]
   (.getEpochSecond ^java.time.Instant now)))


(def UTC "UTC")


(defn in-tz [dt tz-id]
  (t/in dt tz-id))


(def ->date-time t/date-time)


(defn ->instant [dt]
  (t/instant dt))
