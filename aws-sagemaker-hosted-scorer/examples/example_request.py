import boto3

client = boto3.client('sagemaker-runtime')

endpoint_name = "endpoint-name"
content_type = "application/json"
payload = """
{
    "fields": [
        "text_field"
    ],
    "includeFieldsInOutput": [
        "text_field"
    ],
    "rows": [
        [
            "some words"
        ]
    ]
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
