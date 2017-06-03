(ns es-typeahead.core
  (:require [qbits.spandex :as s]
            [faker.name :refer [names]])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, asdf World!"))

(defn index [name id]
  (->> {:url (str "/blog/user/" id)
        :method :put
        :body {:name name}
        :headers {:content-type "application/json"}}
       (s/request (s/client))
       pprint))

(defn count []
  (->> {:url "/blog/user/_count"
        :method :post}
       (s/request (s/client))
       :body
       :count))

(doall (map index (take 100 (names)) (range)))

(->> {:url "/blog/user/_search"
      :method :get
      :body {:query {:match_all {}}}
      :headers {:content-type "application/json"}}
     (s/request (s/client))
     :body
     :hits
     :hits
     (map :_source)
     pprint)
