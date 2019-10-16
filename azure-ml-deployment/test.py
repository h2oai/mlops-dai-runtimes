import requests
import json
import pandas as pd 

service_uri = "http://eab43710-5b28-4228-ae7b-df236b23e6b2.eastus.azurecontainer.io/score"
service_key = "27gZi21I5em5T5sJ4qwpcaTgwIYnhqYq"

headers = {'Content-Type': 'application/json'}


headers['Authorization'] = 'Bearer '+service_key

print(headers)


test_file = pd.read_csv("./mojo/example.csv",index_col=None)

test_sample = json.dumps({"data": test_file.to_json(orient='records')})


response = requests.post(
    service_uri, data=test_sample, headers=headers)
print(response.status_code)
print(response.elapsed)
print(response.json())