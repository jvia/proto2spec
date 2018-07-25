(ns proto2spec.core
  (:require [protobuf.core :as proto]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [spec-tools.data-spec :as ds]
            [clojure.string :as str]))

(defn spec-kw
  "Create a keyword appropriate for use as a spec name.

  Examples:
  - (spec-kw \"a.b.c.d\")  => :a.b.c./d
  - (spec-kw :ns \"name\") => :ns/name
  "
  ([n]
   (let [segments (str/split n #"\.")]
     (spec-kw (str/join "." (butlast segments))
              (last segments))))
  ([ns n]
   (keyword (name ns) (name n))))


(defmulti emit :type)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aggregate Types

(defmethod emit :struct
  [{spec-name :name fields :fields}]
  (let [{spec :spec form :form}
        (ds/spec {:name (spec-kw spec-name)
                  :spec (into {} (for [[field-name field-schema] fields]
                                   [field-name (emit field-schema)]))})]
    (s/def-impl (spec-kw spec-name) form spec)))

(defmethod emit :list
  [{values :values}]
  (st/create-spec {:spec (s/coll-of (emit values) :kind list?)}))

(defmethod emit :set
  [{values :values}]
  (st/create-spec {:spec (s/coll-of (emit values) :kind set?)}))

(defmethod emit :map
  [{keys :keys vals :values}]
  (st/create-spec {:spec (s/map-of (emit keys) (emit vals))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Primitive Types

(defmethod emit :any
  [_]
  (st/create-spec {:spec any? :type :any}))

(defmethod emit :string
  [_]
  (st/create-spec {:spec string? :type :string}))

(defmethod emit :boolean
  [_]
  (st/create-spec {:spec boolean? :type :boolean}))

(def integer-32?
  (s/and integer? #(<= Integer/MIN_VALUE % Integer/MAX_VALUE)))

(defmethod emit :int
  [_]
  (st/create-spec {:spec integer-32? :type :int}))

(defmethod emit :long
  [_]
  (st/create-spec {:spec integer? :type :long}))

(defmethod emit :float
  [_]
  (st/create-spec {:spec float?}))

(defmethod emit :double
  [_]
  (st/create-spec {:spec double?}))

(defmethod emit :enum
  [{values :values}]
  (st/create-spec {:spec values}))

(defmethod emit :byte_string
  [_]
  (st/create-spec {:spec bytes?}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API

(defn refine-with
  "Given a map of spec name to specs, refine the specs."
  [spec-map]
  (doseq [[spec-name refinement] spec-map]
    (if-let [spec (s/get-spec spec-name)]
      (let [{form :form spec :spec} (st/create-spec {:spec (s/and spec refinement)})]
        (s/def-impl spec-name form spec)))))

(defn replace-with
  ""
  [spec-map]
  (doseq [[spec-name substitution] spec-map]
    (if-let [spec (s/get-spec spec-name)]      
      (let [{form :form spec :spec} (st/create-spec {:spec substitution})]
        (s/def-impl spec-name form spec)))))

(defn generate-with
  ""
  [gen-map]
  (doseq [[spec-name gen-fn] gen-map]
    (if-let [spec (s/get-spec spec-name)]      
      (let [{form :form spec :spec} (st/create-spec {:spec spec})]
        (s/def-impl spec-name form (s/with-gen spec gen-fn))))))

(defn proto-spec
  "Generate specs corresponding to an Proto schema. Note that all named
  types mut be generated separately as a top-level call to prevent
  them from clobbering one another.

  Optional keyword arguments:

  :rename       - A namespaced keyword with which to replace the Proto 
                  schema name.

  :refinements  - A map of spec names to a spec or predicate. These are
                  combined with the base spec using clojure.spec/and.

  :replacements - A map of spec names to a spec. This will completely
                  replace the base spec.

  :generators   - A map of spec names to generator functions. This will
                  provide a custom generator for a spec type to better
                  model the  domain."
  ([obj] (proto-spec obj {}))
  ([obj {:keys [rename replacements refinements generators]}]   
   (let [proto-schema (cond-> obj
                        ;; Ensure schema type
                        (not (map? obj))
                        proto/schema
                        ;; Allow for schema rename
                        (some? rename)
                        (assoc :name rename))
         spec-name (emit proto-schema)]

     (when (some? refinements)
       (refine-with refinements))

     (when (some? replacements)
       (replace-with replacements))

     (when (some? generators)
       (generate-with generators))

     spec-name)))




