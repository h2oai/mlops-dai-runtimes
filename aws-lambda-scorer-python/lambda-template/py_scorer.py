import os
import json
import boto3
import botocore
import daimojo.model
import datatable as dt

s3 = boto3.resource('s3')


class ScorerError(Exception):
    """Base Scorer Error"""
    status_code = 500


class InternalError(ScorerError):
    """Internal Error"""
    status_code = 500


class BadRequest(ScorerError):
    """Bad request"""
    status_code = 400


class NotFound(ScorerError):
    """Not found"""
    status_code = 404


def get_object_from_s3(bucket_name, bucket_key):
    try:
        s3.Bucket(bucket_name).download_file(bucket_key, '/tmp/pipeline.mojo')
    except botocore.exceptions.ClientError:
        raise NotFound("Not able to download the mojo file")


def score(body, request_id):
    if body is None or len(body.keys()) == 0:
        raise BadRequest("Invalid request. Need a request body.")

    if 'fields' not in body.keys() or not isinstance(body['fields'], list):
        raise BadRequest("Cannot determine the request column fields")

    if 'rows' not in body.keys() or not isinstance(body['rows'], list):
        raise BadRequest("Cannot determine the request rows")

    get_object_from_s3(os.environ.get('DEPLOYMENT_S3_BUCKET_NAME'), os.environ.get('MOJO_S3_OBJECT_KEY'))
    m = daimojo.model("/tmp/pipeline.mojo")

    if all(feature in body.keys() for feature in m.feature_names):
        raise BadRequest("Bad request body")

    columns_length = len(body['fields'])
    for row in body['rows']:
        if len(row) != columns_length:
            raise BadRequest("Columns length does not match rows length")

    types = {}
    for index, value in enumerate(m.feature_names):
        types[value] = m.feature_types[index]

    # properly define types based on the request elements order
    tmp_frame = dt.Frame(
        [list(x) for x in list(zip(*body['rows']))],
        names=list(types.keys()),
        stypes=list(types.values())
    )

    # fill up the missing values and use the tmp frame to avoid overriding to a lower type
    # example from 'Str32' down to 'Float32/hex'
    d_frame = dt.fread(
        text=tmp_frame.to_csv(),
        columns=list(types.keys()),
        na_strings=m.missing_values
    )

    res = m.predict(d_frame)

    return {
        'id': request_id,
        'score': list(zip(*res.to_list()))
    }


def request_handler(event, context):
    request_id = ''
    try:
        if 'requestContext' not in event.keys() or 'requestId' not in event['requestContext']:
            raise BadRequest("Request does not seem to be relayed through an AWS API Gateway")
        else:
            request_id = event['requestContext']['requestId']

        if event['httpMethod'] != 'POST':
            raise BadRequest("Unsupported http action.")

        if event['resource'] != '/score':
            raise NotFound("Resource not found.")

        if 'body' not in event.keys():
            raise BadRequest("Invalid request. Need a request body.")

        return {
            'isBase64Encoded': False,
            'statusCode': 200,
            'body': json.dumps(score(json.loads(event['body']), request_id))
        }
    except ScorerError as e:
        return {
            'isBase64Encoded': False,
            'statusCode': e.status_code,
            'body': json.dumps({'id': request_id, 'message': str(e)})
        }
    except Exception as exc:
        print(exc)
        return {
            'isBase64Encoded': False,
            'statusCode': 500,
            'body': json.dumps({'id': request_id, 'message': str(exc)})
        }
