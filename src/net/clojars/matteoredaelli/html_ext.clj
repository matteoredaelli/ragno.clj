;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

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

