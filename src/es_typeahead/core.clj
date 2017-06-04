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

  (req "/music" :put {:mappings
                      {:song
                       {:properties
                        {:suggest {:type "completion"}
                         :title {:type "keyword"}}}}})

  (doseq [song (take 100 (names))]
    (req "/music/song" :post {:suggest
                              {:input song}}))

  (->> {:query {:match_all {}}}
       (req "/music/_search" :post)
       :hits
       :hits
       (map :_source))

  (->> {:suggest
        {:song-suggest
         {:prefix "W"
          :completion {:field "suggest"}}}}
       (req "/music/_search" :post)
       :suggest
       :song-suggest
       (map :options))


  )
