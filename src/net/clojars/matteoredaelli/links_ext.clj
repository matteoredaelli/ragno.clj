(ns net.clojars.matteoredaelli.links-ext
  (:require [lambdaisland.uri :refer [uri]]
            [net.clojars.matteoredaelli.uri-ext :as uri-ext]))


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
