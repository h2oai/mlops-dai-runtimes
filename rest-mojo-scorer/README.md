REST Sever Scorer
==================
This is a very simple server that accepts URL/Rest call and produces a score using a MOJO that was created from DAI.

Execution
----------
 
 1. Open a new terminal window :
 ``java -Dai.h2o.mojos.runtime.license.file=license.sig -DModelDirectory=Mojo/ -jar dai-mojo-restserver.jar``

 2. In another new terminal window
 ``curl "http://127.0.0.1:8080/model?name=pipeline.mojo&verbose=true&row=5000,36months,10.65,162.87,10,RENT,24000,VERIFIED-income,AZ,27.65,0,1,0,13648,83.7,0"``

Default Port
-------------
The application uses port 8080, if a different port is required add -Dserver.port=xxxx before the -jar on the command line.

``java -Dai.h2o.mojos.runtime.license.file=license.sig -DServer.port=8989 -jar dai-mojo-restserver.jar``

Any of the Spring Application properties can be overridden in this way.

Security
--------
The example uses Basic Authentication on the /modelsecure end point only.

The parameter -DSecureEndPoint="/**" can be added to the command line, to secure all the end points.

To access a secure end point, the username h2o password h2o123 must be passed.

If you are using a web browser, a logon panel will prompt you.
If using a Rest API call, then pass a base64 encoded username password.
I use PostMan as a tool to setup and test the API call.

URL Requests
-------------
The general format is:

``curl "127.0.0.1:8080/model?name=/tmp/pipeline.mojo&row=5000,36%20months,10.65,162.87,10,RENT,24000,VERIFIED%20-%20income,AZ,27.65,0,1,0,13648,83.7,0"``

Three requests are supported.

``/model``
This is used to pass the row variable, the variable "name" is the model name to invoke.

``/modeljson`` (for DAI) ``/modelh2ojson`` (for h2o) This is used to pass a row as a json object to the server for scoring.

``/modelfeatures``
This lists the model column names for the model name passed as "name".

``/modelstats``
This reports latency statistics on all models

``/modelreload``
This forces all models to be reloaded on the next call and reinitializes the statistics. Only available in the DAIMojoRestServer4 distribution.

``/modelvars``
This provides support to call a model using the feature names (as reported by modelfeatures) as variables in any order on the URL command.

For example: modelvars?name=pipeline.mojo&loan_amnt=5000&term=36months&int_rate=10.65&installment=162.87&emp_length=10&home_ownership=RENT&annual_inc=24000&verification_status=VERIFIED-income&addr_state=AZ&dti=27.65&delinq_2yrs=0&inq_last_6mths=1&pub_rec=0&revol_bal=13648&revol_util=83.7&total_acc=0

``/modeltext``
This calls the /model but returns only the result and not the target variable name, so it can be used in Excel for example.

``/modelsecure``
Same call as /model, except using a basic authentication secured end point.

``/mli``
This calls the klime MLI mojo, without scoring and returns the klime results. Note: using the explainability=true on the /model or /modelvars calls will return both the score and MLI results, in one REST call.

``/score``
This enables the request to be forwarded to the Python HTTP server, this is used to get Shapley reason codes currently. Note very scalable but it works as a workaround.

``/batch`` Uploads a file to be used for scoring, uses the same parameters are /model.
Calling example: ``curl -F "file=@example.csv" http://127.0.0.1:8080/batch?name=diabetes.mojo Add the parameter --spring.servlet.multipart.max-file-size=10MB ``--spring.servlet.multipart.max-request-size=10MB after the jar file to enable uploads over 1MB. See the folder Client for examples of sending the calls to the end point.

``/upload``
This like the batch call, except it returns a csv with the predictions per row.

/qlik This passes a csv style input row and returns a labeled row with the original data and predictions.

For example ``curl "127.0.0.1:8080/qlik?name=pipeline.mojo&verbose=true&row=5000,36months,10.65,162.87,10,RENT,24000,VERIFIED-income,AZ,27.65,0,1,0,13648,83.7,0" loan_amnt,term,int_rate,installment,emp_length,home_ownership,annual_inc,verification_status,addr_state,dti,delinq_2yrs,inq_last_6mths,pub_rec,revol_bal,revol_util,total_acc,bad_loan.0,bad_loan.1 5000,36months,10.65,162.87,10,RENT,24000,VERIFIED-income,AZ,27.65,0,1,0,13648,83.7,0,0.2325363134421583,0.7674636865578417``

``/model2JSON`` Like ``/model`` but returns just the prediction as separate JSON elements for easier parsing by some tools.

URL Variables
*************

name - Absolute path to the model name on the server, default /tmp/pipeline.mojo

row - This is the row data to be scored, add %20 for spaces as per the http protocol requirements.

type - This is the model type, default 2.

verbose - Boolean to enable (true) extra logging on the server standard out, default false.

explainability - Boolean to indicate if MLI results should be returned for this request. Default false.

You can also pass the mojo's feature names as variables, see goCurlVars.sh as an example.

Response Output
----------------
The server will log the latency in nanoseconds of the scoring function to the processes standard out.

The client issuing the request (curl, python, java etc) will receive a JSON response, with one key called "result".

If the result contains multiple lines, then the JSON response is a list within the bodies response, for example the response from a /modelstats request.

Command Line Parameters
------------------------
The following options are also available when starting the Server.

- ``-DModelDirectory=`` This is the full path to the location of the Mojo's to serve.

- ``-DSecureEndPoints=`` The url that will trigger a security challenge (Basic HTTP Auth). Defaults to /modelsecure** so any request requires authentication.

  Example: ``http://127.0.0.1:8443/modelsecure?name=pipeline.mojo&verbose=true&row=5000,36months,10.65,162.87,10,RENT,24000,VERIFIED-income,AZ,27.65,0,1,0,13648,83.7,0``

Default user h2o (-DRestUser=) and password h2o123 (-DRestPass=) for secure requests.

- ``-DScoreHostIP`` IP address to remote Python HTTP server to forward specific requests (/score)

- ``-DScoreLog`` Option to log into a separate log file for audit and control all scoring and MLI requests and results. Default False.

- ``-DExtendedOutput`` Option to write more verbose JSON result string for the result from the call. Default False.

- ``-DDetectShift`` If the row being scored should be compared (number fields only) to see if the data is outside of the range the model was trained on. Default False. Requires the Experiment Summary zip file from DAI to be saved into the mojo directory with the same name as the mojo for example a DAI mojo called pipeline.mojo would have the experiment summary saved as pipeline.mojo.experiment_summary (with NO zip extension).

- ``-DDetectShiftTolerance=`` Float representing a percentage over the training data that shift is allowed before alerting in the logs and via JMX. Default 0.

- ``-DDetectShiftEnforce=`` Boolean to indicate, if the scoring row numeric values are outside of the range trained on to NOT score but return no result.

Questions
----------
Feel free to email me and I will try to help egudgion@h2o.ai
