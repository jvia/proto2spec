(ns photo
  (:require [proto2spec.core :refer [proto-spec]]
            [protobuf.core :as proto]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.string :as str]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as stest])
  (:import [examples Photo]))

;; proto-spec allows you to automatically create Clojure Specs for a
;; given protobuf class.
(proto-spec Photo)


;; We can now see the generated spec for this protobuf
(keys (st/registry #"protobuf.*"))
(comment
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
   :protobuf.examples.photo$Photo/labels))



;; We can validate data in our programs using this generated spec.
(s/valid? :protobuf.examples.photo$Photo/type :tiff)
(comment
  false)


;; We can explain why a larger piece of data is not valid as well. In
;; this case, we can see that our attributes field requires a mapping
;; of string -> string but we have a time attribute with an
;; instant. These can get to be more complicated as the domain model
;; gets more precise.
(s/explain-str :protobuf.examples.photo$Photo/attrs
               {"location" "Spain","time" #inst "2017-01-20T08:30:00Z"})
(comment  
  "In: [\"time\" 1] val: #inst \"2017-01-20T08:30:00.000-00:00\" fails spec: :protobuf.examples.photo$Photo/attrs at: [1] predicate: string?\n")


;; We can even generate data using our spec. Here is an example of
;; generating an entire Photo object. We can use this data to test our
;; code using a technique called generative testing. More on this later.
(def example-photo (gen/generate (s/gen :protobuf.examples.photo/Photo)))
(comment
  {:id 92,
   :path "cl8umsilsjMEeShgoKq9retj",
   :labels
   #{"4zpio73pZNehB9iq" "q8gRdy87MclnGT" "pclBd3v74px9Q2iM7RSf77C5VnQ2"
     "COnCKgW5BayunDosTh4I2Yg" "bo" "JYH8" "zNf34y6q7BoH5WSU"
     "e94YU55UdH0X44S0" "Z560zN" "4d8a6J"},
   :attrs
   {"V82ZbgkeG" "OP49SKU8g88Mj97H7ESQOfnjAHDK",
    "86xP3499V70d4g668i" "2Bli68zc6eJ9qiz30"},
   :tags
   ({:person-id 0,
     :x-coord 0.08023452758789062,
     :y-coord -0.038195669651031494,
     :width -62872784,
     :height -3906}),
   :image
   [50, -113, 99, -108, -99, 112, -69, 47, 88, 25, 59, 47, -58, 92,
    -46, -54, 13, -83, 67, 117, -83, -103],
   :type :png})


;; This object is ready to be serialized into protobuf and sent to
;; other systems for testing as welll.
(->> example-photo
     (proto/create Photo)
     (proto/->bytes))
(comment
  [8, 92, 18, 24, 99, 108, 56, 117, 109, 115, 105, 108, 115, 106, 77,
   69, 101, 83, 104, 103, 111, 75, 113, 57, 114, 101, 116, 106, 26, 20,
   10, 16, 52, 122, 112, 105, 111, 55, 51, 112, 90, 78, 101, 104, 66,
   57, 105, 113, 16, 1, 26, 18, 10, 14, 113, 56, 103, 82, 100, 121, 56,
   55, 77, 99, 108, 110, 71, 84, 16, 1, 26, 32, 10, 28, 112, 99, 108,
   66, 100, 51, 118, 55, 52, 112, 120, 57, 81, 50, 105, 77, 55, 82, 83,
   102, 55, 55, 67, 53, 86, 110, ...])


;; Now we can start to better model our domain. The proto-spec
;; function takes an options map which allows you to modify the
;; generated specs in 3 ways. You can 1) refine a spec, 2) replace a
;; spec, and 3) provide a custom generator. We will see all of these
;; in action.


;; 1) Refinements: Sometimes our domain has stronger requirements than
;; what can be capture in protobuf. In this case, we want our IDs to
;; be positive integers. To capture this fact, we will refine the
;; generated spec. This essentially `s/ands` the provided predicate onto
;; the generated spec.
(def options
  {:refinements
   {:protobuf.examples.photo$Photo/id pos-int?}})

(proto-spec Photo options)
(gen/sample (s/gen :protobuf.examples.photo$Photo/id))
(comment
  (1 1 1 7 7 1 7 169 4 27))


;; 2) Replacements: For some entries, we are looking to capture more
;; specific knowledge. While many of these can be captured
;; as :refinements, it is sometimes easier to just replace the
;; generated spec with our own.
(def options
  {:refinements
   {:protobuf.examples.photo$Photo/id pos-int?}
   :replacements
   ;; End range is exclusive
   {:protobuf.examples.photo.Photo$Tag/y-coord (s/double-in -90.0 91.0)
    :protobuf.examples.photo.Photo$Tag/x-coord (s/double-in -180.0 181.0)}})

(proto-spec Photo options)
(gen/sample (s/gen :protobuf.examples.photo.Photo/Tag) 2)
(comment  
  '({:person-id -1, :x-coord 0.5, :y-coord 0.5, :width -1, :height -1}
    {:person-id 0, :x-coord -2.0, :y-coord 0.5, :width 0, :height 0}))



;; 3) Generators: Sometimes we want the generated data to look more
;; like the domain or our specs are too complicated for the automatic
;; generators to create enough useful data quickly enough. To fix
;; either of these situations, we can provide our own custom
;; generators. In this partiuclar case, we will update the labels
;; generator to produce more domain matching labels.

(def labels-gen
  "This generator creates a set of labels sample from hard coded elements."
  #(gen/set
    (gen/elements ["spain" "food" "holiday" "family"])))

(def options
  {:refinements
   {:protobuf.examples.photo$Photo/id pos-int?}
   :replacements
   ;; End range is exclusive
   {:protobuf.examples.photo.Photo$Tag/y-coord (s/double-in -90.0 91.0)
    :protobuf.examples.photo.Photo$Tag/x-coord (s/double-in -180.0 181.0)}
   :generators
   {:protobuf.examples.photo$Photo/labels labels-gen}})


(proto-spec Photo options)
(gen/sample (s/gen :protobuf.examples.photo$Photo/labels))
(comment
  '(#{}
    #{}
    #{}
    #{"holiday" "family" "spain"}
    #{"food" "holiday" "spain"}
    #{"spain"}
    #{}
    #{"food" "holiday" "family" "spain"}
    #{"food" "holiday" "family" "spain"}
    #{}))


;; Caveats: When refining or overriding a spec it is possible to
;; create something so specific that the default generators have
;; trouble creating random data for them. When that happens, you will
;; need to provide a generator to be able to generate data.


;; For example, let's assume that all of our attributes need to start
;; with `attr-`. The chances that the default string generator will
;; create strings in that format is exceedingly low.

(defn attr-string? [s]
  (str/starts-with? s "attr-"))

(def options
  {:refinements
   {:protobuf.examples.photo$Photo/id pos-int?
    :protobuf.examples.photo$Photo/attrs (s/map-of attr-string? string? :min-count 1 :max-count 3)}
   :replacements
   ;; End range is exclusive
   {:protobuf.examples.photo.Photo$Tag/y-coord (s/double-in -90.0 91.0)
    :protobuf.examples.photo.Photo$Tag/x-coord (s/double-in -180.0 181.0)}
   :generators
   {:protobuf.examples.photo$Photo/labels labels-gen}})

(proto-spec Photo options)
(gen/sample (s/gen :protobuf.examples.photo$Photo/attrs))
(comment
  "ExceptionInfo Couldn't satisfy such-that predicate after 100 tries.  clojure.core/ex-info (core.clj:4739)")

;; To fix this, we can provide a generate which will make keys
;; attribute maps in the correct format.

(def attr-key-gen  
  #(gen/bind
    (gen/such-that
     not-empty
     (gen/string-alphanumeric))
    (fn [s] (gen/return (str "attr-" s)))))

(def attr-gen
  #(gen/bind
    (gen/list (gen/tuple (attr-key-gen)
                         (gen/string-alphanumeric)))
    (fn [kvs] (gen/return (into {} kvs)))))

(def options
  {:refinements
   {:protobuf.examples.photo$Photo/id pos-int?
    :protobuf.examples.photo.Photo$Tag/height pos-int?
    :protobuf.examples.photo.Photo$Tag/width pos-int?
    :protobuf.examples.photo$Photo/attrs (s/map-of attr-string? string? :min-count 1 :max-count 3)}
   :replacements
   ;; End range is exclusive
   {:protobuf.examples.photo.Photo$Tag/y-coord (s/double-in :min 0.0 :max 100.0 :infinite? false :NaN? false)
    :protobuf.examples.photo.Photo$Tag/x-coord (s/double-in :min 0.0 :max 100.0 :infinite? false :NaN? false)}
   :generators
   {:protobuf.examples.photo$Photo/labels labels-gen
    :protobuf.examples.photo$Photo/attrs attr-gen}})

(proto-spec Photo options)
(gen/sample (s/gen :protobuf.examples.photo$Photo/attrs))
(comment
  '({"attr-f" "x0"}
    {"attr-c" ""}
    {"attr-04z" "W", "attr-9" "Hf"}
    {"attr-Tp" "9we", "attr-1F" ""}
    {"attr-J2" ""}
    {"attr-0GH6wq" "5", "attr-611Ex62" "Ued8B", "attr-gLcj4A9" "c"}
    {"attr-Hw" "L"}
    {"attr-067" "T9d488", "attr-k8q" "5xoC0"}
    {"attr-811t65A1" "9Zm", "attr-tI29v" "2hEI0EsA"}
    {"attr-xu5892t03jRv" "gB84f8ZZ5YnG0RNu",
     "attr-Y3htJEAY" "e3rngss6KTl83MB4J9aS09"}))


;; Now with our more fine-tune specs, we can better validate, conform,
;; and sample data.
(gen/generate (s/gen :protobuf.examples.photo/Photo))
(comment
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
   :type :png})



;; With specs modeling our domain, we can now write functions in terms
;; of this domain and use generative testing to write property based
;; assertions about our functions.

;; Here we create a function that returns the scaled coordinates
;; of the image using its height and width. We also write a spec
;; definition for the function. This will be used to generated test
;; input and validate output. This can get pretty sophisticated so it
;; is worth looking at the documentation for what can be done.

;; In this particular example, we have a little bug where mixed up
;; height and width.

(defn coordinates
  [{x :x-coord y :y-coord height :width width :height}]
  (let [scaled-x (Math/round (* (/ x 100.0) width))
        scaled-y (Math/round (* (/ y 100.0) height))]
    [scaled-x scaled-y]))

(s/fdef coordinates
  ;; The function recieves a tag as an argument
  :args (s/cat :tag :protobuf.examples.photo.Photo/Tag)
  :ret (s/tuple nat-int? nat-int?)
  :fn #(and (<= (-> % :ret first) (-> % :args :tag :width))
            (<= (-> % :ret second) (-> % :args :tag :height))))


;; We now instrument the function and run generative tests against
;; it. Very quickly we see that there are issues. When stest/check
;; finds an error, it will try to shrink the input to find the
;; smallest possible instance of that error. You can see and elided
;; output below in the comment.
(stest/instrument `coordinates)
(stest/summarize-results (stest/check `coordinates))
(comment
  {:failure
   {:clojure.spec.alpha/problems
    [{:path [:fn],
      :pred
      (clojure.core/fn
        [%]
        (clojure.core/and
         (clojure.core/<=
          (clojure.core/-> % :ret clojure.core/first)
          (clojure.core/-> % :args :tag :width))
         (clojure.core/<=
          (clojure.core/-> % :ret clojure.core/second)
          (clojure.core/-> % :args :tag :height)))),
      :val
      {:args
       {:tag
        {:person-id 0, :x-coord 1.0, :y-coord 1.0, :width 1, :height 2}},
       :ret [2 1]}}]}}
  {:total 1, :check-failed 1})





;; Now that we know the error, we can fix our function.
(defn coordinates
  [{x :x-coord y :y-coord width :width height :height}]
  (let [scaled-x (Math/round (* (/ x 100.0) width))
        scaled-y (Math/round (* (/ y 100.0) height))]
    [scaled-x scaled-y]))

(stest/summarize-results (stest/check `coordinates))
(comment
  {:sym photo/coordinates}
  {:total 1, :check-passed 1})


;; See https://clojure.org/guides/spec for more information on what is possible.
