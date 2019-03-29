import boto3

client = boto3.client('sagemaker-runtime')

endpoint_name = "tomk-dai-mojo-test-2"
content_type = "application/octet-stream"
payload = """
{
"LIMIT_BAL": "20000",
"SEX": "2",
"EDUCATION": "2",
"MARRIAGE": "1",
"AGE": "24",
"PAY_0": "-2",
"PAY_2": "2",
"PAY_3": "-1",
"PAY_4": "-1",
"PAY_5": "-2",
"PAY_6": "-2",
"BILL_AMT1": "3913",
"BILL_AMT2": "3102",
"BILL_AMT3": "689",
"BILL_AMT4": "0",
"BILL_AMT5": "0",
"BILL_AMT6": "0",
"PAY_AMT1": "0",
"PAY_AMT2": "689",
"PAY_AMT3": "0",
"PAY_AMT4": "0",
"PAY_AMT5": "0",
"PAY_AMT6": "0"
}
"""

response = client.invoke_endpoint(
    EndpointName=endpoint_name, 
    ContentType=content_type,
    Body=payload
    )

print(response)

response_body = response['Body']
print(response_body.read().decode('utf-8'))


#
# In this example's case running against a MOJO built on the creditcard.csv data set, the following response is given by the endpoint:
#

# {'ResponseMetadata': {'RequestId': '83881d78-0ab8-41ea-9871-a0783604225c', 'HTTPStatusCode': 200, 'HTTPHeaders': {'x-amzn-requestid': '83881d78-0ab8-41ea-9871-a0783604225c', 'x-amzn-invoked-production-variant': 'variant-name-1', 'date': 'Fri, 29 Mar 2019 23:51:03 GMT', 'content-type': 'application/json;charset=UTF-8', 'content-length': '105'}, 'RetryAttempts': 0}, 'ContentType': 'application/json;charset=UTF-8', 'InvokedProductionVariant': 'variant-name-1', 'Body': <botocore.response.StreamingBody object at 0x7f07dcb58b38>}
# {"default payment next month.1":"0.5060160136169216","default payment next month.0":"0.4939839863830784"}

