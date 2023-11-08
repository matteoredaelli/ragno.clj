(ns matteoredaelli.social-tagger-test
  (:require [clojure.test :refer :all]
            [net.clojars.matteoredaelli.social-tagger :refer :all]))


(deftest facebook-test
  (testing "tag facebook"
    (is (= (list {:facebook "wikipedia"})
           (tag-link "http://www.facebook.com/wikipedia")
           ))
    (is (= (list {:facebook "wikipedia"})
           (tag-link "https://www.facebook.com/wikipedia")
           ))
    ))

(deftest youtube-test
  (testing "tag youtube"
    (is (= (list {:youtube "wikipedia"})
           (tag-link "http://www.youtube.com/wikipedia")
           ))
    (is (= (list {:youtube "wikipedia"})
           (tag-link "https://www.youtube.com/wikipedia")
           ))
    (is (= (list {:youtube "wikipedia"})
           (tag-link "https://www.youtube.com/user/wikipedia")
           ))
    ))
