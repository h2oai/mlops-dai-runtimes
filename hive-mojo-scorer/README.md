# Driverless AI Model Deployment: Apache Hive

Driverless AI models can be deployed in Hive with HiveQL (HQL). Using Driverless AI and Hive together lets you quickly build and deploy models. 

The following User-Defined Function (UDF) lets the model name be used as part of the query, which means that a single UDF can dynamically load and score multiple models.

This UDF have been tested with both Hive and Beeline shells.

## Hive UDF Scorer Download

Download the Hive scorer from the [Custom Scorers download page](https://s3.amazonaws.com/artifacts.h2o.ai/releases/ai/h2o/dai-custom-scorers/DAI-1.8.9/index.html).

## Environment Variables

The Hive UDF uses environment variables to pass the Driverless AI license and model name. It also uses an environment variable to specify whether feature labels are included as part of the output.

### Variables:  
- ```DRIVERLESS_AI_LICENSE_FILE```: Path to license file 
- ```DRIVERLESS_AI_MODEL_NAME```: Overrides mojo name (default: pipeline.mojo)  
- ```DRIVERLESS_AI_MODEL_OUTPUT_LABELS``` (true | false): Specify whether to output target labels (default: true)  
    Example: select daiPredict(col1, col2).

## Deployment

### Requirements

1. Driverless AI MOJO: Copy the MOJO file to the `models/` directory.

2. Driverless AI license: Provided through the partnership portal. Copy the license to the license.sig file in the `license/` directory, then run `export DRIVERLESS_AI_LICENSE_FILE=/path/to/license.sig`

3. Java Development Kit 1.8: Run `java -version` to verify that the available JVM on your platform has JDK 1.8 installed. If the output does not show JDK 1.8, download a 1.8 JDK for your environment. The following sites provide current builds for free:
    * https://www.azul.com/downloads/zulu-community/
    * https://openjdk.java.net/install/

4. H2O Runtime Drivers: Runtime drivers required to run the scorer are located in the `lib/` directory. 

5. JAR File: DAI-Mojo-Hive JAR file in `scorer/` directory.

### Example

Run the following in the Hive console.

**Note**: Use the runtime that matches the Driverless AI version the model was created with.

```
hive> add jar mojo2-runtime-1.5.5.jar;  
Added [mojo2-runtime-1.5.5.jar] to class path  
Added resources: [mojo2-runtime-1.5.5.jar]  

hive> add jar DAIMojoRunner_Hive.jar;  
Added [DAIMojoRunner_Hive.jar] to class path  
Added resources: [DAIMojoRunner_Hive.jar]  

hive> list jar;  
mojo2-runtime-1.5.5.jar  
DAIMojoRunner_Hive.jar  

hive> create temporary function daiPredict as 'daimojorunner_hive.daiPredict';  

hive> set DRIVERLESS_AI_LICENSE_FILE=license.sig;  
hive> select id, daiPredict(loan_amnt, term, int_rate, installment, emp_length, home_ownership, annual_inc, verification_status, addr_state, dti, delinq_2yrs, inq_last_6mths, pub_rec, revol_bal, revol_util, total_acc) from lcdata where addr_state='CA' and loan_amnt>34000;  
1068159	"bad_loan.0"='0.7421794315246639', "bad_loan.1"='0.2578205684753361’

hive> describe function extended daiPredict;  
Call a Driverless AI Mojo for scoring prediction  
Synonyms: daipredict  
```
	