(ns flx.awspricy
  (:require [clojure.data.json :as json]
            [malli.core :as m]
            [malli.transform :as mt]
            [datomic.client.api :as d]
            [camel-snake-kebab.core :as csk]))

"
1. Fetch json to a local disk
2. Read from local disk

{1 2} - Read from the url instead?
3. Transform relevant maps into the datomic model +
4. Insert datomic model into Datomic - Transact schema into datomic +

TODO:
0. Use both data :OnDemand and :Reserved?
1. Transform keys: add namespace, convert to kebab case (during transformation?)
2. Adjust the schema keys and/or use it for transformation
3. (Perhaps) Avoid writing a schema by hand and write malli->datomic schema conversion function
4. Write generative tests (after conversation/feedback/access to original codebase)
"

(def data
  (json/read-json
    (slurp "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonDynamoDB/current/index.json")))

(def on-demand-data
  (->> data
       :terms
       :OnDemand
       (map #(-> % second first second :priceDimensions first second))
       (mapv #(update-keys % csk/->kebab-case))))

(defn unit [u]
  (case u
    "ACU-Hr" :unit/acu-hour
    "API Calls" :unit/api-call
    "CR-Hr" :unit/cr-hour
    "GB" :unit/gb
    "GB-Mo" :unit/gb-month
    "Hrs" :unit/hour
    "IOPS-Mo" :unit/iops-month
    "IOs" :unit/ios
    "Quantity" :unit/quantity
    "vCPU-Hours" :unit/vcpu-hour
    "vCPU-Months" :unit/vcpu-month
    false))

;; TODO
(def price-dimensions
  [:map
   [:rate-ccode {:id? true} 'string?]
   [:description 'string?]
   [:begin-range {:optional true} 'string?]
   [:end-range {:optional true} 'string?]
   [:unit ['keyword? {:decode/json {:leave unit}}]]
   [:price-per-unit [:map [:USD ['double?
                                 {:decode/json
                                  {:leave 'read-string}}]]]]])

(def schema (m/schema price-dimensions))

(def strict-json-transformer
  (mt/transformer
    mt/strip-extra-keys-transformer
    mt/json-transformer))

(defn apply-schema
  "Apply schema to a map"
  [m]
  (m/decode schema m strict-json-transformer))

(defn transform-data
  "Apply malli transformations to a vector of maps"
  [data]
  (->> data
       (map apply-schema)
       (filter :unit)))                                     ;; TODO: avoid manual filtering by a proper use of malli schema

(def datomic-schema
  [{:db/ident       :rate-code
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :description
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :begin-range
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :end-range
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :unit
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :price-per-unit
    :db/isComponent true
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :USD
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}


   {:db/ident :unit/acu-hour}
   {:db/ident :unit/api-call}
   {:db/ident :unit/cr-hour}
   {:db/ident :unit/gb-month}
   {:db/ident :unit/gb}
   {:db/ident :unit/hour}
   {:db/ident :unit/iops-month}
   {:db/ident :unit/ios}
   {:db/ident :unit/quantity}
   {:db/ident :unit/vcpu-hour}
   {:db/ident :unit/vcpu-month}])

(def client (d/client {:server-type :dev-local
                       :storage-dir :mem
                       :system      "dev"}))

(d/create-database client {:db-name "awspricy"})
;;(d/delete-database client {:db-name "awspricy"})

(def conn (d/connect client {:db-name "awspricy"}))

(d/transact conn {:tx-data datomic-schema})

(d/transact conn {:tx-data (transform-data on-demand-data)})

(def awspricy-q '[:find ?unit ?price ?desc
                  :where
                  [_ :description ?desc]
                  [_ :unit ?u]
                  [?u :db/ident ?unit]
                  [_ :price-per-unit ?p]
                  [?p :USD ?price]])

(take 10 (d/q awspricy-q (d/db conn)))
