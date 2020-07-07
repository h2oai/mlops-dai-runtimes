import pandas as pd
import requests


# Assumes that the python script is running in same location as rest scorer
# Change <localhost> to proper ip address to get scores from remote scorer
URL = "http://localhost:8080/model/score"
HEADERS = "Content-Type: application/json"

sample_df = pd.read_csv("/path/to/data.csv")


# Score single row at a time
for i, row in sample_df.iterrows():
    # Replace any null values with stringified version
    # Else will result in json parsing errors
    row = row.fillna("NaN")

    payload = {"fields": list(sample_df.columns), "rows": [list(row)]}
    res = requests.post(URL, data=payload, headers=HEADERS)

    print(res.content)


# Score all rows in single request
# Replace any null values with stringified version
# Else will result in json parsing errors
sample_df = sample_df.fillna("NaN")
payload = {"fields": list(sample_df.columns), "rows": sample_df.values.tolist()}
res = requests.post(URL, data=payload, headers=HEADERS)

print(res.content)

