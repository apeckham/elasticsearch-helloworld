(ns es-typeahead.core
  (:require [qbits.spandex :as s]
            [faker.name :refer [names]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, asdf World!"))

(->> {:url "/blog/user/dilbert"
      :method :put
      :body {:name (first (names))}
      :headers {:content-type "application/json"}}
     (s/request (s/client))
     pprint)

(->> {:url "/blog/user/_search"
      :method :get
      :body {:query {:match_all {}}}
      :headers {:content-type "application/json"}}
     (s/request (s/client))
     :body
     :hits
     :hits
     pprint)
