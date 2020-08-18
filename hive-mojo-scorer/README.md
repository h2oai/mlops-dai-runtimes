Hive UDF Scorer
================
Use a Driverless AI Model in Hive via HQL.Using DAI and then this program enables them to quickly build and use models. The UDF enables the model name to use used as part of the query, this means the UDF can dynamically load and score from a single UDF, this helps in deployment otherwise a UDF per model might be required. This UDF have been used with both Hive and Beeline shells.

Execution
----------


``
hive> add jar mojo2-runtime-1.7.1.jar;  
Added [mojo2-runtime-1.5.5.jar] to class path  
Added resources: [mojo2-runtime-1.5.5.jar]  

hive> add jar dai-mojo-hive.jar;  
Added [dai-mojo-hive.jar] to class path  
Added resources: [dai-mojo-hive.jar]  

hive> list jar;  
mojo2-runtime-1.7.1.jar  
DAIMojoRunner_Hive.jar  

hive> create temporary function daiPredict as 'daimojorunner_hive.daiPredict';  

hive> set DRIVERLESS_AI_LICENSE_FILE=license.sig;  
hive> select id, daiPredict(loan_amnt, term, int_rate, installment, emp_length, home_ownership, annual_inc, verification_status, addr_state, dti, delinq_2yrs, inq_last_6mths, pub_rec, revol_bal, revol_util, total_acc) from lcdata where addr_state='CA' and loan_amnt>34000;  
1068159	"bad_loan.0"='0.7421794315246639', "bad_loan.1"='0.2578205684753361’

hive> describe function extended daiPredict;  
Call a Driverless AI Mojo for scoring prediction  
Synonyms: daipredict  
``

Variables
**********
- ``DRIVERLESS_AI_LICENSE_FILE`` path to license file 
- ``DRIVERLESS_AI_MODEL_NAME`` to override mojo name (default: pipeline.mojo)
- ``DRIVERLESS_AI_MODEL_OUTPUT_LABELS`` (true | false) to output target labels (default: true)

Example: select daiPredict(col1, col2).