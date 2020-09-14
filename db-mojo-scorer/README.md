# Database Scorer
Some customers need to select rows from a Database, score and write back the predictions.Using DAI and then this program enables them to quickly build and use models.

Supports following databases
1. Postgres DB
2. Microsoft SQL Server 12
3. TerraData
4. RedShift
5. Oracle
6. DB2
7. Azure Data Warehouse
8. Snowflake
9. ElasticSearch
  
## Running 
- You must have a DAI license to score, so add the license (via command line) in one of the standard ways, as a parameter, as a environment vailable for example:
    `Dai.h2o.mojos.runtime.license.file=/Driverless-AI/license/license.sig`
    ```
    The license can also be passed:
    1. Environment variable DRIVERLESS_AI_LICENSE_FILE : A location of file with a license.
    2. Environment variable DRIVERLESS_AI_LICENSE_KEY : A license key.
    3. System properties of JVM (-D option) 'ai.h2o.mojos.runtime.license.file' : A location of  
    4. System properties of JVM (-D option) 'ai.h2o.mojos.runtime.license.key' : A license key.
    5. Classpath: The license is loaded from resource called '/license.sig' The default resource name can be changed via system property 'ai.h2o.mojos.runtime.license.filename'
    ```
       
- To run the database scorer, you can directly run the executable jar:
    ```
    java -Dpropertiesfilename={PATH_TO_PROPERTIES_FILE} -jar build/libs/dai-mojo-db-{YOUR_CURRENT_VERSION}.jar
    ```

### Properties Files

Using the flag -Dpropertiesfilename= multiple configuration files can be created for different models or databases, just specify the properties file to use at runtime.

#### Sample Properties File

```
ModelName=pipeline.mojo
SQLConnectionString=jdbc:postgresql://192.168.1.171:5432/LendingClub SQLUser=postgres
SQLPassword=aDJvaDJvCg==
SQLPrompt=
SQLKey=id
SQLPrediction=
SQLSelect=select id, loan_amnt, term, int_rate, installment, emp_length, home_ownership, annual_inc, verification_status, addr_state, dti, delinq_2yrs, inq_last_6mths, pub_rec, revol_bal, revol_util, total_acc from "import".loanstats4
SQLWrite=update "import".loanstats4 set where id=
```
The above will score using **pipeline.mojo** and use the key 'id' to update the column name from the mojo into the table, to `bad_loan.0` and `bad_loan.1` must be in the table schema.

If SQLPrediction was set to ModelPrediction (for example) then `bad_loan.0` would be written to the table column ModelPrediction.


#### Usage
```
java -Dpropertiesfilename=DAI-Mojo-DB/properties/DAIMojoRunner_DB.properties-Snowflake -jar DAI-Mojo-DB/build/libs/dai-mojo-db-2.30.jar
```

### Runtime Flags

While the Database and Mojo configuration is in the properties file, the command line does have some flags

 - `-Dverbose=true` | false{default}       
      verbosity flag
 - `-Doverride=true`| false{default}     
      bypass check if running multithreads on a table without an index, as the table would be in a random order.
 - `-Dwait=true`| false: {default}       
      causes the program to wait for Enter once the model and threads are ready, used for demos.
 - `-Dstats=true` | false{default}  
      writes performance statistics at completion.
 - `-Dinspect=true`| false{default}  
      prints model details and suggest SQLSelect based on model as well as runtime java memory settings.
 - `-Dcapacity=` Number of available Cores+33%{default}  
     Size of buffer queue.
 - `-Dthreads=` Number of available Cores{default}
 - `-Dpropertiesfilename=DAIMojoRunner_DB.properties`{default}        
      allows multiple properties files to be created for different models and databases

Example:
```
java -Derrors=true -Dverbose=true -Dthreads=2 -Dcapacity=4000 -Xms2g -Xmx10g -Dpropertiesfilename={PATH_TO_PROPERTIES_FILE} -jar build/libs/dai-mojo-db-{YOUR_CURRENT_VERSION}.jar
```

## Deployment Tricks

- Its important to also have a key for each row being scored, this is because the scorer will execute in parallel (based on the -Dthreads= parameter) 
so updating the databases will be faster if a unique key is used for each row (use a customer id or even the row number) for the SQLKey statement.

- The option -Dinsepect=true can be used to generate the SQL based on the model, this helps when a large number of database columns are required.

    This example assumes the model is called `pipeline.mojo` but could be change using the properties file.
    ```
    # input
    java -Dinspect=true -jar DAIMojoRunner_DB.jar
    
    # output
    Details of Model: pipeline.mojo
    UUID: b2fce6c1-6ddc-4c78-8355-f437945a613c Input Features
    0 = Name: loan_amnt Type: Float32
    1 = Name: term Type: Str
    2 = Name: int_rate Type: Float32
    3 = Name: installment Type: Float32
    4 = Name: emp_length Type: Float32
    5 = Name: home_ownership Type: Str
    6 = Name: annual_inc Type: Float32
    7 = Name: verification_status Type: Str
    8 = Name: addr_state Type: Str
    9 = Name: dti Type: Float32
    10 = Name: delinq_2yrs Type: Float32
    11 = Name: inq_last_6mths Type: Float32
    12 = Name: pub_rec Type: Float32
    13 = Name: revol_bal Type: Float32
    14 = Name: revol_util Type: Float32
    15 = Name: total_acc Type: Float32
    Output Features
    0 = Name: bad_loan.0 Type: Float64
    1 = Name: bad_loan.1 Type: Float64
    Suggested configuration for properties file:
    
    select {add-table-index}, loan_amnt, term, int_rate, installment, emp_length, home_ownership, annual_inc, verification_status, addr_state, dti, delinq_2yrs, inq_last_6mths, pub_rec, revol_bal, revol_util, total_acc from {add-table-name}
    
    update {add-table-name} set where {add-table-index}=
    ```


**Note** 
> Change the values in {} above and manually test before using them in the program.

> The System has 16GB available physically. This program is using 0GB Consider adjusting `-Xms` and `-Xmx` to no more than 12GB
The System has 8 Processors

## Handling Passwords

### Support For Encrypted Password

- Store the encrypted password in the DAIMojoRunner_DB.properties
	
- Encrypt the password from Linux command line: (type in the password):
  ```
  echo h2oh2o | base64 -i –
  ```  
- Encrypt the password from Windows PowerShell (password passed as h2oh2o)
  ```
  [convert]::toBASe64String([Text.Encoding]::UTF8.GetBytes("h2oh2o"))
  ```
  Save the encrypted string to the SQLPassword parameter

- The password encryption can also be performed by setting the SQLPrompt to encrypt then run 
  ```
  java -jar DAIMojoRunner_DB.jar
  ```     
  you will be prompted to enter the JDBC password, it will then be encrypted so you can paste it into the properties file. 

### Prompting for password  

- Set `SQLPrompt= Enables` prompting for the password in the `DAIMojoRunner_DB.properties`
	
- Add the username to the SQLUser line make sure SQLPassword, is set `SQLPassword=Set` `SQLPrompt=true`

#### Using Environment Variables

If the execution environment has variables with the values for the parameters they can be used in the properties file. The setting `EnvVariables=true` must be in the properties file ideally the first line or before substitution is needed.

Then use the environment variables like this:    
```
jdbc:sqlserver://'$ADW_HOST';databaseName='$ADW_DB';user='$ADW_USER';password='$ADW_PWD'
```

**Variable names start with $ and are delimited by '** 

For example:  

```
# IF THE ENVIRONMENTS ARE...  
  
ADW_DB=MyDatabase    
ADW_HOST=127.0.0.1:5033 
ADW_PWD=%@#$^*   
ADW_USER=Admin 

# AT RUNTIME IT WOULD SUBSTITUTE TO...

jdbc:sqlserver://127.0.0.1:5033;databaseName=MyDatabase;user=Admin;password=%@#$^*

``` 
 
#### Using Windows Active Directory

- Some scoring environments want to use the credentials of the user executing the scoring rather than a service account.

- The score can use the AD for this, follow these steps: 

    1. Add to the command line:
       ```
       -Djava.library.path=\sqljdbc_6.0\auth
       ```            
    2. Add to the SQLConnectionString
        ```
        ;integratedSecurity=true
        ```
   
For Example 

The command line would look like this: 
```
java -Xms4g -Xmx4g -Dlogging=true -Dpropertiesfilename=DAIMojoRunner_DB.properties-MSSQL-Auth -Dai.h2o.mojos.runtime.license.file={PATH_TO_LICENSE}/license.sig -Djava.library.path=C:\sqljdbc_6.0\enu\auth\x64 -cp {PATH_TO_JAR}/dai-mojo-db-2.30.jar ai.h2oai.mojos.db.daimojorunner_db.DAIMojoRunner_DB   
```
The SQLConnectionString in the properties file would look like this: 
```
SQLConnectionString=jdbc:sqlserver://192.168.1.173:1433;databaseName=LendingClub;integratedSecurity=true
```
The SQLUser and SQLPassword would be commented out.

## Result

The DB scorer stores the score/prediction for each row from the given dataset in a file called `results.csv` in the main directory.

```
java -Derrors=true -Dverbose=true -Dthreads=2 -Dcapacity=4000 -Xms2g -Xmx10g -Dpropertiesfilename=properties/DAIMojoRunner_DB.properties-Snowflake -jar build/libs/dai-mojo-db-2.30.jar

H2O DAI Mojo Database Scoring v2.30
 
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry EnvVariables not found in properties file, setting to default
Property EnvVariables = false
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry IgnoreEmptyStrings not found in properties file, setting to default
Property IgnoreEmptyStrings = False
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry IgnoreChecks not found in properties file, setting to default
Property IgnoreChecks = False
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry IgnoreSQLChecks not found in properties file, setting to default
Property IgnoreSQLChecks = False
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry ForceConnection not found in properties file, setting to default
Property ForceConnection = False
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry ForceConnectionClass not found in properties file, setting to default
Property ForceConnectionClass = 
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property ModelName = Mojo/pipeline.mojo
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLConnectionString = jdbc:snowflake://h20_ai_partner.snowflakecomputing.com/?warehouse=DEMO_WH&db=LENDINGCLUB&schema=PUBLIC
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLUser = VIYENGAR
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLPassword = MjMwN0xlZ2hvcm4=
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLPrompt not found in properties file, setting to default
Property SQLPrompt = 
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLSelect = select id, loan_amnt, term, int_rate,  installment,  emp_length,  home_ownership,  annual_inc,  verification_status,  addr_state,  dti,  delinq_2yrs,  inq_last_6mnths,  pub_rec,  revol_bal,  revol_util,  total_acc  from "LENDINGCLUB";
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLWrite = CSV
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLWriteCSVFilename = results.csv
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLWriteBatch not found in properties file, setting to default
Property SQLWriteBatch = 
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLWriteBatchSize not found in properties file, setting to default
Property SQLWriteBatchSize = 100000
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLKey = id
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLPrediction not found in properties file, setting to default
Property SQLPrediction = 
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLPredictionLabel not found in properties file, setting to default
Property SQLPredictionLabel = 
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLFieldSeperator not found in properties file, setting to default
Property SQLFieldSeperator = ,
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry InternalFieldSeperator not found in properties file, setting to default
Property InternalFieldSeperator = ,
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry SQLWriteFieldSeperator not found in properties file, setting to default
Property SQLWriteFieldSeperator = ,
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLStartScript = snowflake/SnowflakeStart.sql
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Property SQLEndScript = snowflake/SnowflakeUpdate.sql
Using properties file: properties/DAIMojoRunner_DB.properties-Snowflake
Propertry Threshold not found in properties file, setting to default
Property Threshold = 
Using model Mojo/pipeline.mojo
TQ: 4000 2
Queue Capacity Starting 4000
Connection string used seperate SQL parameters
Reader connected to database! {}
Running SQL Script snowflake/SnowflakeStart.sql
Script init completed 
Invoking SQL Script snowflake/SnowflakeStart.sql
select count(*) from "LENDINGCLUB"
 
COUNT(*)	
39029	
delete from "RESULTS"
 
select count(*) from "RESULTS"

COUNT(*)	
 0	
SQL Script completed 
Reading ResultSet for select id, loan_amnt, term, int_rate,  installment,  emp_length,  home_ownership,  annual_inc,  verification_status,  addr_state,  dti,  delinq_2yrs,  inq_last_6mnths,  pub_rec,  revol_bal,  revol_util,  total_acc  from "LENDINGCLUB";
ID,  LOAN_AMNT,  TERM,  INT_RATE,  INSTALLMENT,  EMP_LENGTH,  HOME_OWNERSHIP,  ANNUAL_INC,  VERIFICATION_STATUS,  ADDR_STATE,  DTI,  DELINQ_2YRS,  INQ_LAST_6MNTHS,  PUB_REC,  REVOL_BAL,  REVOL_UTIL,  TOTAL_ACC
RAW ResultSet: 1077501
RAW ResultSet: 5000
RAW ResultSet: 36 months
RAW ResultSet: 10.65
RAW ResultSet: 162.87
RAW ResultSet: 10
RAW ResultSet: RENT
RAW ResultSet: 24000

...

Script init completed 
Invoking SQL Script snowflake/SnowflakeUpdate.sql
put file://results.csv @~/staged

source	target	source_size	target_size	source_compression	target_compression	status	encryption	message	
results.csv	results.csv.gz	1783130	814416	NONE	GZIP	UPLOADED	ENCRYPTED		
copy into RESULTS from @~/staged file_format = ( type = CSV)

remove @~/stages/results.csv.gz
 
name	result	
select count(*) from "RESULTS"
 
COUNT(*)	
39029	
SQL Script completed 
```
**Note** 

 >  Please create a `results.csv` file in the main directory (if it doesn't exist already). 

 > It is important for the execution of the above command to have a `results.csv` file in the main dir to save the data processed by the scorer as the code **will not create the file itself if it doesn't exist**.
