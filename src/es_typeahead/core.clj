(ns es-typeahead.core
  (:require [qbits.spandex :as s])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, asdf World!"))

(->> {:url "/blog/user/dilbert"
      :method :put
      :body {:name "Dilbert Brown"}
      :headers {:content-type "application/json"}}
     (s/request (s/client)))

(->> {:url "/blog/user/_search"
      :method :get
      :body {:query {:match_all {}}}
      :headers {:content-type "application/json"}}
     (s/request (s/client))
     :body
     :hits
     :hits
     pprint)
