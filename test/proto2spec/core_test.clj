(ns proto2spec.core-test
  (:require [clojure.test :refer :all]
            [proto2spec.core :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.set :as set])
  (:import [examples Photo]))

(deftest emit-test
  (is (not= (emit {:type :long}) (emit {:type :int}))
      "Int/Long must generated different spec for Clojure."))

(deftest proto-spec-test
  (proto-spec Photo)
  (is (every? set? (map first (s/exercise :protobuf.examples.photo$Photo/labels)))
      "Generated spec should have the extension type if present.")

  (proto-spec Photo {:refinements {:protobuf.examples.photo$Photo/id even?}})
  (is (every? even? (map first (s/exercise :protobuf.examples.photo$Photo/id)))
      "A refinement should be added to the existing spec to make it more specific")

  (proto-spec Photo {:replacements {:protobuf.examples.photo$Photo/id #{0}}})
  (is (every? zero? (map first (s/exercise :protobuf.examples.photo$Photo/id)))
      "A replacement should be completely override the default spec.")

  (proto-spec Photo {:generators {:protobuf.examples.photo$Photo/attrs
                                  #(gen/map
                                    (s/gen #{"date" "location" "iso" "aperture" "size"})
                                    (gen/string-alphanumeric)
                                    {:min-elements 1})}})
  (is (every? #(set/subset? % #{"date" "location" "iso" "aperture" "size"})
              (map (comp keys first) (s/exercise :protobuf.examples.photo$Photo/attrs)))
      "A generator should replace a spec's default generator."))


