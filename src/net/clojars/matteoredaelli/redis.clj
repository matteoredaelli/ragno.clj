;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.clojars.matteoredaelli.redis
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [net.clojars.matteoredaelli.ragno :as ragno]
            [taoensso.carmine :as car :refer [wcar]]
            ))

(defn read-edn-file [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))
  

(defonce my-conn-pool (car/connection-pool {})) 

(defn redis-worker
  [redis ragno-options]
  (def queue-name (:queue-name redis))
  (def queue-name-star (str queue-name "*"))
  (def my-conn-spec {:uri (:url redis)})
  (def my-wcar-opts {:pool my-conn-pool :spec my-conn-spec})
  (defmacro wcar* [& body] `(car/wcar ~my-wcar-opts ~@body))

  ;;(wcar* (car/ping))
  
  (def listener
    (car/with-new-pubsub-listener my-conn-spec
      {
       queue-name (fn f1
                [msg]
                (println (get msg 0) "->" (get msg 2))
                (if (= "message" (get msg 0))
                  (let [url (get msg 2)
                        resp (ragno/surf url ragno-options)]
                    (println resp)
                    (wcar * (car/set url (json/write-str resp)))
                    )
                 )
               )
       queue-name-star  (fn f2 [msg] (println "Pattern match: " msg))}
      (car/subscribe  queue-name)
      (car/psubscribe queue-name-star)
      ))

  (Thread/sleep 1000000000)
  (wcar* (car/close-listener listener))
  ;;(car-mq/stop my-worker)
  )

(defn cli
  [opts]
  (let
      [config-file (:config-file opts)
       config (read-edn-file config-file)
       ragno-options (:ragno-options config)
       redis (:redis config)]
    (redis-worker redis ragno-options)))
