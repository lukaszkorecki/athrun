(ns athrun.cron-test
  (:require
    [athrun.cron :as cron]
    [clojure.test :refer [deftest testing is]]))


(deftest parsing-and-extracting-data
  (testing "parses and expression and calculates next execution values"
    (let [schedule (cron/parse "0 /5 * * * ?"
                               #_ "0 0,5,10,15,20,25,30,35,40,45,50,55  * * * ?")
          now #inst "2022-03-13T10:20:15"]
      (is (= "2022-03-13T10:25:00Z" (str (cron/next-execution now schedule))))
      (is (= 285 (cron/next-execution-in-seconds now schedule)))
      (is (= 300 (cron/execution-interval-in-seconds now schedule)))))
  (testing "complicated expression starting using exact beginning of minute"
    (let [schedule (cron/parse "0 0,5,10,15,20,25,30,35,40,45,50,55  * * * ?")
          now #inst "2022-03-13T10:20:00"]
      (is (= "2022-03-13T10:25:00Z" (str (cron/next-execution now schedule))))
      (is (= 0 (cron/next-execution-in-seconds now schedule)))
      (is (= 300 (cron/execution-interval-in-seconds now schedule)))))


  (testing "short schedules - ever 2s"
    (let [schedule (cron/parse "/2 * * * * ? *")
          now #inst "2022-03-13T10:20:15"]
      (is (= "every 2 seconds" (cron/explain schedule)))
      (is (= "2022-03-13T10:20:16Z" (str (cron/next-execution now schedule))))
      (is (= 1 (cron/next-execution-in-seconds now schedule)))
      (is (= 2 (cron/execution-interval-in-seconds now schedule)))))

  (testing "short schedules - ever 5s"
    (let [schedule (cron/parse "/5 * * * * ? *")
          now #inst "2022-03-13T10:20:15"]
      (is (= "every 5 seconds" (cron/explain schedule)))
      (is (= "2022-03-13T10:20:20Z" (str (cron/next-execution now schedule))))
      (is (= 0 (cron/next-execution-in-seconds now schedule)))
      (is (= 5 (cron/execution-interval-in-seconds now schedule)))))

  (testing "long schedules"
    (let [schedule (cron/parse "0 0 2 * * ?")
          now #inst "2022-03-13T10:20:15"]
      (is (= "at 02:00" (cron/explain schedule)))
      (is (= "2022-03-14T02:00:00Z" (str (cron/next-execution now schedule))))
      (is (= 56385 (cron/next-execution-in-seconds now schedule)))
      (is (= (* 24 60 60) (cron/execution-interval-in-seconds now schedule))))))


(deftest testing-core-schedules
  (testing "core loop schedule"
    (let [schedule (cron/parse "0 /5 * * * ?")]
      (is (cron/matches-schedule? #inst "2021-02-03T00:05:00" schedule))
      (is (not (cron/matches-schedule? #inst "2021-02-03T00:05:03" schedule)))
      (is (not (cron/matches-schedule? #inst "2021-02-03T00:03:00" schedule))))))


(deftest bugs
  (testing "short schedules get interval of 1"
    (testing "every 3s starting at 0th second"
      (let [schedule (cron/parse "/3 * * * * ?")
            now  #time/instant "2022-03-16T19:45:00.357135Z"]

        (is (= "every 3 seconds" (cron/explain schedule)))
        (is (= "2022-03-16T19:45:03Z" (str (cron/next-execution now schedule))))
        (is (= 0 (cron/next-execution-in-seconds now schedule)))
        (is (= 3 (cron/execution-interval-in-seconds now schedule)))))

    (testing "every 3s starting so that next falls at 0th second"
      (let [schedule (cron/parse "/3 * * * * ?")
            now  #time/instant "2022-03-16T19:44:57.357135Z"]

        (is (= "every 3 seconds" (cron/explain schedule)))
        (is (= "2022-03-16T19:45:00Z" (str (cron/next-execution now schedule))))
        (is (= 0 (cron/next-execution-in-seconds now schedule)))
        (is (= 3 (cron/execution-interval-in-seconds now schedule)))))))
