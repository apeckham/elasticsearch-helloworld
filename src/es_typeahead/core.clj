(ns es-typeahead.core
  (:gen-class)
  (:require [clojure.core.async :as async]
            [faker.name :refer [names]]
            [qbits.spandex :as s]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, asdf World!"))

(def client (s/client))

(defn req [url method body]
  (->> {:url url
        :method method
        :body body
        :headers {:content-type "application/json"}}
       (s/request client)
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

(comment "easy indexing"
         (doall (map index (take 10 (names))))

         (count)

         (match-all))

(comment "completion suggester"

         (req "/music" :delete nil)

         (req "/music" :put {:mappings
                             {:song
                              {:properties
                               {:suggest {:type "completion"}
                                :title {:type "keyword"}}}}})

         (doseq [song (take 1000 (names))]
           (req "/music/song" :post {:additional "payload"
                                     :suggest
                                     {:input song}}))

         (let [{:keys [input-ch output-ch]} (s/bulk-chan client)
               action (fn [name]
                        [{:index {:_index "music"
                                  :_type "song"}}
                         {:additional "payload2"
                          :suggest {:input name}}])]

           (async/put! input-ch (mapcat action (take 10000 (names))))
           (async/close! input-ch)
           (loop []
             (when-let [[job response] (async/<!! output-ch)]
               (if (-> response :body :errors)
                 (throw (Exception. "Bulk index failed")))
               (recur))))

         (:count (req "/music/song/_count" :post nil))

         (->> {:query {:match_all {}}}
              (req "/music/_search" :post)
              :hits
              :hits
              (map :_source))

         (defn suggest [prefix]
           (->> {:suggest
                 {:song-suggest
                  {:prefix prefix
                   :completion {:field "suggest"}}}}
                (req "/music/_search" :post)
                :suggest
                :song-suggest
                first
                :options
                (map :_source)))

         (suggest "A")


         )
