;   Copyright (c) Matteo Redaelli at gmail dot com. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

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

