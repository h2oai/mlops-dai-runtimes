
# Deploying Driverless AI Models to Production

This documentation lists some of the scenarios to deploy **[Driverless AI](http://docs.h2o.ai/driverless-ai/latest-stable/docs/userguide/index.html)** models to production and provides guidlines to create deployment templates for the same.

The final model from a Driverless AI experiment can be exported as either a  **[MOJO scoring pipeline](http://docs.h2o.ai/driverless-ai/latest-stable/docs/userguide/scoring-mojo-pipelines.html)** or a **[Python scoring pipeline](http://docs.h2o.ai/driverless-ai/latest-stable/docs/userguide/scoring-standalone-python.html)**. The Mojo scoring pipeline comes with a **pipline.mojo** file with a Java or C++ runtime. This can be deployed in **any** environment that supports Java or C++. The Python scoring pipeline comes with the scoring **whl** file for deployment purposes. 

Below, is a list of the deployment scenerios for Driverless AI pipelines for Real-time, Batch or Stream scoring.

```mermaid
graph LR;
  
  A["Realtime Scoring (single or multi-rows)"]-->AA[DAI MOJO Pipeline with Java Runtime];
  A-->AB[DAI MOJO Pipeline with C++ Runtime];
  A-->AC[DAI PYTHON Scoring Pipeline];
  
  AA-->AAA["As REST Server"]
  AA-->AAB["As AWS Lambda"]
  AA-->AAC["As GCP Cloud Run"]
  AA-->AAD["As AzureML"]
  AA-->AAE["As library"]
  AA-->AAF["As Apache NiFi "]
  AA-->AAG["As Apache Flink"]
  
  AB-->ABA["As library"]
  AB-->ABB["As Apache NiFi "]
  
  AC-->ACA["As REST Server"]
  AC-->ACB["As Apache NiFi"]
  
  click AAA "./local-rest-scorer"
  click AAB "./aws_lambda_scorer"
  click AAC "./gcp"
  click AAF "https://github.com/james94/dai-deployment-examples/tree/mojo-nifi/mojo-nifi"
  click AAG "https://github.com/h2oai/dai-deployment-examples/tree/master/mojo-flink"
  
```
```mermaid
graph LR;
  
  A["Batch Scoring"]-->AA[DAI MOJO Pipeline with Java Runtime];
  A-->AB[DAI MOJO Pipeline with C++ Runtime];
  A-->AC[DAI PYTHON Scoring Pipeline];
  
  AA-->AAA["As Apache Spark batch"]
  AA-->AAB["As library"]
  AA-->AAC["As Hive UDF"]
  AA-->AAD["As DB scorer"]
  AA-->AAE["As Apache NiFi"]
  AA-->AAF["As Apache Flink"]
  AA-->AAG["As Snowflake Workflow"]
  
  AB-->ABA["As library"]
  AB-->ABB["As Apache NiFi"]
  
  AC-->ACA["As library"]
  AC-->ACB["As Apache NiFi"]
  
  click AAA "http://docs.h2o.ai/sparkling-water/3.0/latest-stable/doc/deployment/load_mojo_pipeline.html#loading-and-score-the-mojo"
  click AAD "./sql-jdbc-scorer"
  click AAE "https://github.com/james94/dai-deployment-examples/tree/mojo-nifi/mojo-nifi"
  click AAF "https://github.com/h2oai/dai-deployment-examples/tree/master/mojo-flink"
  click AAG "https://www.youtube.com/watch?v=EMpGVer01WE"
```

```mermaid
graph LR;
  
  A["Stream Scoring"]-->AA[DAI MOJO Pipeline with Java Runtime];
  
  AA-->AAA["As Apache Spark Stream"]
  AA-->AAB["As Task Queue "]
  AA-->AAC["As Active MQ"]
  AA-->AAD["As Kafka Topic"]
  
  click AAD "https://github.com/h2oai/dai-deployment-examples/blob/master/mojo-flink/daimojo-flink-kafka.md"
  
```