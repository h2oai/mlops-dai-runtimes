Database Scorer
===============
Some customers need to select rows from a Database, score and write back the predictions.Using DAI and then this program enables them to quickly build and use models.

Supports following databases
 1. PostgreSQL
 2. SQL Server
 3. Elasticsearch JDBC
 4. Microsoft SQL Server
 5. Snowflake Database
 6. Teradata JDBC

Execution
---------

Step 1 
*******

You must have a DAI license to score, so add the license (via command line) in one of the standard ways, as a parameter, as a environment vailable for example:

`Dai.h2o.mojos.runtime.license.file=/Driverless-AI/license/license.sig`

The license can also be passed:

 1. Environment variable DRIVERLESS_AI_LICENSE_FILE : A location of file with a license.
 2. Environment variable DRIVERLESS_AI_LICENSE_KEY : A license key.
 3. System properties of JVM (-D option) 'ai.h2o.mojos.runtime.license.file' : A location of  
 4. System properties of JVM (-D option) 'ai.h2o.mojos.runtime.license.key' : A license key.
 5. Classpath: The license is loaded from resource called '/license.sig' The default resource name can be changed via system property 'ai.h2o.mojos.runtime.license.filename'

Step 2
*******

Execution in simple, run:

``java -Dpropertiesfilename=<property-file> -jar dai-mojo-db.jar``

Execution Options
-----------------

Properties Files
*****************

Using the flag -Dpropertiesfilename= multiple configuration files can be created for different models or databases, just specify the properties file to use at runtime.

Runtime Flags
*************

While the Database and Mojo configuration is in the properties file, the command line does have some flags

 - ``-Dverbose=true`` |false{default}
 - ``-Doverride=true`` |false{default} bypass check if running multithreads on a table without an index, as the table would be in a random order.
 - ``-Dwait=true`` |false: {default} causes the program to wait for Enter once the model and threads are ready, used for demos.
 - ``-Dstats=true`` |false{default} writes performance statistics at completion.
 - ``-Dinspect=true``|false{default} prints model details and suggest SQLSelect based on model as well as runtime java memory settings.
 - ``-Dthreads=`` Number of available Cores{default}
 - ``-Dpropertiesfilename=DAIMojoRunner_DB.properties``{default} allows multiple properties files to be created for different models and databases

all flags in bold font are used in the execution command example above

Result
------

The DB scorer stores the score/prediction for each row from the given dataset in a file called results.csv in the main directory.


**Note** 

 >  Please create a results.csv file in the main directory (if it doesn't exist already). It is important for the execution command to have a results.csv file in the main dir to save the data processed by the scorer as the code will not create the file itself if it doesn't exist.

