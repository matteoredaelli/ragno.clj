(ns matteoredaelli.uri-ext-test
  (:require [clojure.test :refer :all]
            [lambdaisland.uri :refer [uri join]]
            [net.clojars.matteoredaelli.uri-ext :refer :all]))

(def url1     "https://www.redaelli.org/path/to/file.php?p1=v1&p2=v2")
(def uri1 (uri url1))
(def url1-bis "https://www.redaelli.org/path/to/file2.php?p1=v1&p2=v2")
(def uri1-bis (uri url1-bis))
(def url2 "https://www.redaelli.org:443/path/to/file2.php?p1=v1&p2=v2")
(def uri2 (uri url2))

(deftest same-field?-test
  (testing "same-field"
    (and (is (same-field? uri1 uri1 :schema))
         (is (same-field? uri1 uri1 :host))
         (is (same-field? uri1 uri2 :schema))
         (is (same-field? uri1 uri2 :host))
         (is (not (same-field? uri1 uri1-bis :path)))
         )))

(deftest same-website?-test
  (testing "same-website"
    (and (is (same-website? uri1 uri1))
         (is (same-website? uri1 uri1-bis))
         (is (not (same-website? uri1 uri2)))
         )))

(deftest filter-external-links-test
  (testing "external-links"
    (and (is (filter-internal-links [url1 url1-bis url2] url1))
         (is (filter-external-links [url1 url1-bis url2] url1))
         )))

(deftest get-domain-test
  (testing "get-domain"
    (and
     (is (= "https://www.redaelli.org" (get-domain-url (uri "https://www.redaelli.org/"))))
     (is (= "https://www.redaelli.org" (get-domain-url (uri "https://www.redaelli.org/a/b?a=1"))))
     (is (= "https://www.redaelli.org" (get-domain-url (uri "https://www.redaelli.org/a.html#a"))))
    )))
