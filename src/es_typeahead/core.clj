(ns es-typeahead.core
  (:require [qbits.spandex :as s]
            [faker.name :refer [names]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, asdf World!"))

(defn req [url method body]
  (->> {:url url
        :method method
        :body body
        :headers {:content-type "application/json"}}
       (s/request (s/client))
       :body))

(defn index [name]
  (pprint (req "/blog/user" :post {:name name})))

(defn count []
  (:count (req "/blog/user/_count" :post nil)))

(defn match-all []
  (->> {:query {:match_all {}}}
       (req "/blog/user/_search" :get)
       :hits
       :hits
       (map :_source)
       pprint))

(comment
  (doall (map index (take 10 (names))))

  (count)

  (match-all)

  )
