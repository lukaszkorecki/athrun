(ns athrun.middleware
  (:require
    [clojure.tools.logging :as log]
    [lockjaw.protocol :as lock]))


(defn with-lock [scheduled-fn]
  (fn [{:keys [lock] :as component}]
    (if (lock/acquire! lock)
      (do
        (log/debugf "lock-status=acquired name=%s" (:name lock))
        (scheduled-fn component))
      (log/debugf "lock-status=none name=%s" (:name lock)))))


(defn with-logging [scheduled-fn]
  (fn [component]
    (try
      (log/debugf "running %s" (meta scheduled-fn))
      (scheduled-fn component)
      (log/debugf "done %s" (meta scheduled-fn))
      (catch Throwable err
        (log/errorf err "execution failed %s" (meta scheduled-fn))))))
