proto2spec Guide
================

To get started run this command to run Clojure ([install instruction](https://clojure.org/guides/getting_started)) with the library:
```sh
$ clojure -Sdeps '{:deps {io.gamayun/proto2spec {:mvn/version "0.2.0"}}}'
Downloading: io/gamayun/proto2spec/0.2.0/proto2spec-0.2.0.pom from https://clojars.org/repo/
Downloading: io/gamayun/proto2spec/0.2.0/proto2spec-0.2.0.jar from https://clojars.org/repo/
Clojure 1.9.0
user=>
```

The proto-spec function allows you to automatically create Clojure
Specs for a given protobuf class. We will start out by importing some
libraries as well at the protobuf class.
```clojure
user=> (require '[proto2spec.core :refer [proto-spec]]
               '[protobuf.core :as proto]
               '[clojure.spec.alpha :as s]
               '[spec-tools.core :as st]
               '[clojure.string :as str]
               '[clojure.spec.gen.alpha :as gen]
               '[clojure.spec.test.alpha :as stest])
nil
user=> (import '[examples Photo])
examples.Photo
```

We can use the `proto/schema` function to view the protobuf schema.
```clojure
user=> (proto/schema Photo)
{:type :struct,
 :name "protobuf.examples.photo.Photo",
 :fields
 {:id {:type :int},
  :path {:type :string},
  :labels {:type :set, :values {:type :string}},
  :attrs
  {:type :map, :keys {:type :string}, :values {:type :string}},
  :tags
  {:type :list,
   :values
   {:type :struct,
    :name "protobuf.examples.photo.Photo.Tag",
    :fields
    {:person-id {:type :int},
     :x-coord {:type :double},
     :y-coord {:type :double},
     :width {:type :int},
     :height {:type :int}}}},
  :image {:type :byte_string},
  :type {:type :enum, :values #{:png :gif :jpeg}}}}
```


Now we will generate the specs and view them in the registry.
```clojure
user=> (proto-spec Photo)
:protobuf.examples.photo/Photo
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


We can validate data in our programs using this generated spec.
```clojure
user=> (s/valid? :protobuf.examples.photo$Photo/type :tiff)
false
```


We can explain why a larger piece of data is not valid as well. In
this case, we can see that our attributes field requires a mapping of
`string -> string` but we have a time attribute with an
instant. These can get to be more complicated as the domain model
gets more precise.
```clojure
user=> (s/explain :protobuf.examples.photo$Photo/attrs
                 {"location" "Spain","time" #inst "2017-01-20T08:30:00Z"})
In: ["time" 1] val: #inst "2017-01-20T08:30:00.000-00:00" fails spec: :protobuf.examples.photo$Photo/attrs at: [1] predicate: string?

```

We can even generate data using our spec. Here is an example of
generating an entire Photo object. We can use this data to test our
code using a technique called generative testing. More on this later.
```clojure
user=> (def example-photo (gen/generate (s/gen :protobuf.examples.photo/Photo)))
#'user/example-photo
user=> example-photo
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
 :type :png}
```


This object is ready to be serialized into protobuf and sent to
other systems for testing as welll.
```clojure
user=> (->> example-photo (proto/create Photo) (proto/->bytes))
[8, 92, 18, 24, 99, 108, 56, 117, 109, 115, 105, 108, 115, 106, 77,
 69, 101, 83, 104, 103, 111, 75, 113, 57, 114, 101, 116, 106, 26, 20,
 10, 16, 52, 122, 112, 105, 111, 55, 51, 112, 90, 78, 101, 104, 66,
 57, 105, 113, 16, 1, 26, 18, 10, 14, 113, 56, 103, 82, 100, 121, 56,
 55, 77, 99, 108, 110, 71, 84, 16, 1, 26, 32, 10, 28, 112, 99, 108,
 66, 100, 51, 118, 55, 52, 112, 120, 57, 81, 50, 105, 77, 55, 82, 83,
 102, 55, 55, 67, 53, 86, 110, ...]
```

Now we can start to better model our domain. The proto-spec function
takes an options map which allows you to modify the generated specs in
3 ways. You can 1) refine a spec, 2) replace a spec, and 3) provide a
custom generator. We will see all of these in action.


Refinements
-----------

Sometimes our domain has stronger requirements than
what can be capture in protobuf. In this case, we want our IDs to
be positive integers. To capture this fact, we will refine the
generated spec. This essentially `s/and`'s the provided predicate onto
the generated spec.
```clojure
user=> (def options
        {:refinements
         {:protobuf.examples.photo$Photo/id pos-int?}})
#'user/options
user=> (proto-spec Photo options)
:protobuf.examples.photo/Photo
user=> (gen/sample (s/gen :protobuf.examples.photo$Photo/id))
(1 1 1 7 7 1 7 169 4 27)
```


Replacements
------------

For some entries, we are looking to capture more
specific knowledge. While many of these can be captured
as :refinements, it is sometimes easier to just replace the
generated spec with our own.
```clojure
user=> (def options
        {:replacements
         {:protobuf.examples.photo.Photo$Tag/y-coord 
          (s/double-in :min 0.0 :max 100.0 :infinite? false :NaN? false)
          :protobuf.examples.photo.Photo$Tag/x-coord 
          (s/double-in :min 0.0 :max 100.0 :infinite? false :NaN? false)}})
#'user/options
user=> (proto-spec Photo options)
:protobuf.examples.photo/Photo
user=> (gen/sample (s/gen :protobuf.examples.photo.Photo/Tag) 2)
({:person-id -1, :x-coord 0.5, :y-coord 0.5, :width -1, :height -1}
 {:person-id 0, :x-coord 2.0, :y-coord 0.5, :width 0, :height 0})
```


Generators
----------

Sometimes we want the generated data to look more like the domain or
our specs are too complicated for the automatic generators to create
enough useful data quickly enough. To fix either of these situations,
we can provide our own custom generators. In this partiuclar case, we
will update the labels generator to produce more domain matching
labels.

```clojure
user=> (def labels-gen
        #(gen/set
          (gen/elements ["spain" "food" "holiday" "family"])))
#'user/labels-gen
user=> (def options
        {:generators
         {:protobuf.examples.photo$Photo/labels labels-gen}})
#'user/options
user=> (proto-spec Photo options)
:protobuf.examples.photo/Photo
user=> (gen/sample (s/gen :protobuf.examples.photo$Photo/labels))
(#{}
 #{}
 #{}
 #{"holiday" "family" "spain"}
 #{"food" "holiday" "spain"}
 #{"spain"}
 #{}
 #{"food" "holiday" "family" "spain"}
 #{"food" "holiday" "family" "spain"}
 #{})
```

*Caveats*: When refining or overriding a spec it is possible to create
something so specific that the default generators have trouble
creating random data for them. When that happens, you will need to
provide a generator to be able to generate data.

For example, let's assume that all of our attributes need to start
with `attr-`. The chances that the default string generator will
create strings in that format is exceedingly low.

```clojure
user=> (defn attr-string? [s]
        (str/starts-with? s "attr-"))
#'user/attr-string?
user=> (def options
        {:refinements
         {:protobuf.examples.photo$Photo/attrs 
          (s/map-of attr-string? string? :min-count 1 :max-count 3)}})
#'user/options
user=> (proto-spec Photo options)
:protobuf.examples.photo/Photo
user=> (gen/sample (s/gen :protobuf.examples.photo$Photo/attrs))
"ExceptionInfo Couldn't satisfy such-that predicate after 100 tries.
  clojure.core/ex-info (core.clj:4739)"
```

To fix this, we can provide a generate which will make keys attribute
maps in the correct format.

```clojure
user=> (def attr-key-gen
        #(gen/bind
          (gen/such-that
           not-empty
           (gen/string-alphanumeric))
          (fn [s] (gen/return (str "attr-" s)))))
#'user/attr-key-gen
user=> (def attr-gen
        #(gen/bind
          (gen/list (gen/tuple (attr-key-gen)
                               (gen/string-alphanumeric)))
          (fn [kvs] (gen/return (into {} kvs)))))
#'user/attr-gen
user=> (def options
        {:refinements
         {:protobuf.examples.photo$Photo/attrs
          (s/map-of attr-string? string? :min-count 1 :max-count 3)}         
         :generators
         {:protobuf.examples.photo$Photo/attrs attr-gen}})
#'user/options
user=> (proto-spec Photo options)
:protobuf.examples.photo/Photo
user=> (gen/sample (s/gen :protobuf.examples.photo$Photo/attrs))
({"attr-f" "x0"}
 {"attr-c" ""}
 {"attr-04z" "W", "attr-9" "Hf"}
 {"attr-Tp" "9we", "attr-1F" ""}
 {"attr-J2" ""}
 {"attr-0GH6wq" "5", "attr-611Ex62" "Ued8B", "attr-gLcj4A9" "c"}
 {"attr-Hw" "L"}
 {"attr-067" "T9d488", "attr-k8q" "5xoC0"}
 {"attr-811t65A1" "9Zm", "attr-tI29v" "2hEI0EsA"}
 {"attr-xu5892t03jRv" "gB84f8ZZ5YnG0RNu",
  "attr-Y3htJEAY" "e3rngss6KTl83MB4J9aS09"})
```

Now with our more fine-tuned specs, we can better validate, conform,
and sample data.

Function Specs
--------------

With specs modeling our domain, we can now write functions in terms of
this domain and use generative testing to write property based
assertions about our functions.

Here we create a function that returns the scaled coordinates of the
image using its height and width. We also write a spec definition for
the function. This will be used to generated test input and validate
output. This can get pretty sophisticated so it is worth looking at
the documentation for what can be done.

In this particular example, we have a little bug where mixed up
height and width.
```clojure
user=> (defn coordinates
        [{x :x-coord y :y-coord height :width width :height}]
        (let [scaled-x (Math/round (* (/ x 100.0) width))
              scaled-y (Math/round (* (/ y 100.0) height))]
          [scaled-x scaled-y]))
#'user/coordinates
user=> (s/fdef coordinates
        ;; Receives a Tag as an argument
        :args (s/cat :tag :protobuf.examples.photo.Photo/Tag)
        ;; Returns a pair of natural integers
        :ret (s/tuple nat-int? nat-int?)
        ;; x <= width and y <= height
        :fn #(and (<= (-> % :ret first) (-> % :args :tag :width))
                  (<= (-> % :ret second) (-> % :args :tag :height))))
user/coordinates
```

We now instrument the function and run generative tests against
it. Very quickly we see that there are issues. When `stest/check`
finds an error, it will try to shrink the input to find the
smallest possible instance of that error. You can see and elided
output below in the comment.
```clojure
user=> (stest/instrument `coordinates)
[user/coordinates]
user=> (stest/summarize-results (stest/check `coordinates))
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
{:total 1, :check-failed 1}
```

Now that we know the error, we can fix our function.
```clojure
user=> (defn coordinates
        [{x :x-coord y :y-coord width :width height :height}]
        (let [scaled-x (Math/round (* (/ x 100.0) width))
              scaled-y (Math/round (* (/ y 100.0) height))]
          [scaled-x scaled-y]))
#'user/coordinates
user=> (stest/summarize-results (stest/check `coordinates))
{:sym photo/coordinates}
{:total 1, :check-passed 1}
```


See https://clojure.org/guides/spec for more information on what is
possible.
