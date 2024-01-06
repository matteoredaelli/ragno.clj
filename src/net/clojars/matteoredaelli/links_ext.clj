;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns net.clojars.matteoredaelli.links-ext
  (:require [lambdaisland.uri :refer [uri]]
            [net.clojars.matteoredaelli.uri-ext :as uri-ext]))


(defn cleanup-wwwN
  [links]
  (map #(clojure.string/replace % #"www\d+\." "www."))
       links)
  
(defn remove-fragments
  [links]
  (map #(uri-ext/remove-fragment (uri %))
       links)
  )
(defn remove-links-with-fragment
  [links]
  (filterv #(not (clojure.string/includes? % "#")) links)
  )

(defn remove-empty-links
  [links]
  (filterv #(not (= % ""))
           links)
  )

(defn filter-email-links
  [links]
  (filterv #(clojure.string/starts-with? % "mailto:")
           links))

(defn remove-links-with-mailto
  [links]
  (filterv #(not (clojure.string/starts-with? %
                                              "mailto:"))
           links)
  )

(defn filter-external-links
  [links address]
  (let [address_uri (uri address)]
    (filterv #(not (uri-ext/same-website? (uri %) address_uri))
             links)
    )
  )

(defn filter-internal-links
  [links address]
  (filterv #(uri-ext/same-website? (uri %)
                                   (uri address))
           links)
  )

(defn get-domain-links
  [links]
  (map #(uri-ext/get-domain-url (uri %))
       links)
  )
