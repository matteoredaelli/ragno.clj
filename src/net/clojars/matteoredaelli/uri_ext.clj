(ns net.clojars.matteoredaelli.uri-ext
  (:require [lambdaisland.uri :refer [uri join]]))

;;; URI
(defn same-field?
  [uri1 uri2 field]
  (= (field uri1) (field uri2)))

(defn same-website?
  [uri1 uri2]
  (and (same-field? uri1 uri2 :host)
       (same-field? uri1 uri2 :port)
       (same-field? uri1 uri2 :scheme)))

(defn sub-uri?
  [uri1 uri2]
  (and (same-website? uri1 uri2)
       (clojure.string/starts-with? (:path uri2) (:path uri1))
       (same-field? uri1 uri2 :port)
       (same-field? uri1 uri2 :scheme)))


(defn get-domain-url
  [uri1]
  (str (assoc uri1 :path nil :fragment nil :query nil)))

