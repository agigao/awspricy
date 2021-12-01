# AWSPricy

A very rough implementation of the task - ETL process for Amazon AWS Dynamo DB price data (json) 

In essence:
- It fetches the data over the url
- transforms data using malli transformers
- loads into datomic

> malli/datomic schema lacks namespace for the moment.

- At the end query included for the demonstration, 10 records from datomic.
```clojure
[:unit/gb 0.187 "$0.11400 per GB of data exported in Asia Pacific (Mumbai)"]
[:unit/gb-month 0.1254 "$0.11000 per GB of data exported in EU (Ireland)"]
[:unit/gb 0.242 "$0.11400 per GB of data exported in Asia Pacific (Sydney)"]
[:unit/gb 0.121 "$0.13090 per GB of data exported in Africa (Cape Town)"]
[:unit/gb-month 0.1309 "$0.11000 per GB of data exported in Canada (Central)"]
[:unit/gb-month 0.2508 "$0.13471 per GB-Month of IA storage in Africa (Cape Town)"]
[:unit/gb-month 0.17829 "$0.12540 per GB of data exported in Asia Pacific (Hong Kong)"]
[:unit/gb 0.112 "$0.3 per GB-Month of storage used"]
[:unit/gb-month 0.28 "$0.171 per GB of data restored"]
[:unit/gb-month 0.228 "$0.11550 per GB of data exported in EU (Milan)"]
```

Cheers,\
Giga
