(ns net.clojars.matteoredaelli.html-ext
  (:import (org.jsoup Jsoup)))

(defn extract-link-data [link]
  (let [address (.attr link "abs:href")]
     address))

(defn extract-head-meta-content [soup name value]
  (let [filter (format "head > meta[%s=\"%s\"]" name value)]
    (.attr (.select soup filter) "content")))

(defn extract-element-text [soup element]
   ( apply str (.text (.select soup element))))

(defn extract-links [soup]
  (let [links (.select soup "a")]
    (distinct (mapv extract-link-data links))))

