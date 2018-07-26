# proto2spec

This library converts Protobuf into Clojure Spec definitions.

[![Clojars Project](https://img.shields.io/clojars/v/io.gamayun/proto2spec.svg)](https://clojars.org/io.gamayun/proto2spec)

## Usage

:wave: A fuller tutorial of what can be done exists [here](docs/guide.md).

To get started run this command to run Clojure with the library:
```sh
$ clojure -Sdeps '{:deps {io.gamayun/proto2spec {:mvn/version "0.2.0"}}}'
Downloading: io/gamayun/proto2spec/0.2.0/proto2spec-0.2.0.pom from https://clojars.org/repo/
Downloading: io/gamayun/proto2spec/0.2.0/proto2spec-0.2.0.jar from https://clojars.org/repo/
Clojure 1.9.0
user=>
```

Import the required libraries as well the example.Photo protobuf
class. With this, we can now auto-create the specs using `proto-spec`.

```clojure
user=> (require '[proto2spec.core :refer [proto-spec]]
               '[clojure.spec.alpha :as s]
               '[spec-tools.core :as st]
               '[clojure.spec.gen.alpha :as gen])
nil
user=> (import '[examples Photo])
examples.Photo
user=> (proto-spec Photo)
:protobuf.examples.photo/Photo
```


You can now check out the specs we just created
```clojure
user=> (keys (st/registry #"protobuf.*"))
(:protobuf.examples.photo$Photo/tags
 :protobuf.examples.photo/Photo
 :protobuf.examples.photo$Photo/type
 :protobuf.examples.photo.Photo$Tag/person-id
 :protobuf.examples.photo.Photo/Tag
 :protobuf.examples.photo.Photo$Tag/width
 :protobuf.examples.photo.Photo$Tag/y-coord
 :protobuf.examples.photo$Photo/id
 :protobuf.examples.photo.Photo$Tag/height
 :protobuf.examples.photo$Photo/attrs
 :protobuf.examples.photo$Photo/image
 :protobuf.examples.photo$Photo/path
 :protobuf.examples.photo.Photo$Tag/x-coord
 :protobuf.examples.photo$Photo/labels)
```


We can now validate data as well as generate it:
```clojure
user=> (s/valid?  :protobuf.examples.photo$Photo/id 1)
true
user=> (gen/generate (s/gen :protobuf.examples.photo/Photo))
{:id 308212705,
 :path "9a",
 :labels #{"food" "family" "spain"},
 :attrs
 {"attr-xH8KX4tg699n3OkutiM8t9Dk9xKb2as1O"
  "sE6G11N1A7CVWpCMP207Cuz8vaL4cvx0D40knq96648j78J05t9r91MlF",
  "attr-0INNgB4u0ULGs20S7K" "i6PSxuJVEd57TZysd5TicFttzV2T"},
 :tags
 '({:person-id 255,
    :x-coord -0.0,
    :y-coord 4.145965576171875,
    :width 168,
    :height 81}
   {:person-id -143579278,
    :x-coord 1.2296951413154602,
    :y-coord 12.0,
    :width 9,
    :height 20931}
   {:person-id -2076853,
    :x-coord 0.02734375,
    :y-coord 16.0,
    :width 126764459,
    :height 6}
   {:person-id -6,
    :x-coord 0.216796875,
    :y-coord 1.5,
    :width 28302,
    :height 992734}),
 :image [127, 74, 48],
 :type :png}
```
