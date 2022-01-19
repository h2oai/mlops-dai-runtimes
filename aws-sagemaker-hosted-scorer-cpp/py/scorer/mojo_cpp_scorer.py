from flask import Flask, request
from flask_restful import Resource, Api
import logging
import json
import os
import threading
import daimojo.model
import datatable as dt
import pandas as pd

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
api = Api(app)


class MojoPipeline(object):
    _model = None
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if MojoPipeline._instance is None:
            with MojoPipeline._lock:
                if MojoPipeline._instance is None:
                    MojoPipeline._instance = super(MojoPipeline, cls).__new__(cls)
                    MojoPipeline._instance.setup()
        return MojoPipeline._instance

    def setup(self):
        mojo_file_path = os.getenv('MOJO_FILE_PATH')
        self._model = daimojo.model(mojo_file_path)

    def get_feature_names(self):
        """Return feature names"""

        return self._model.feature_names

    def get_types(self):
        """Get column types"""

        types = {}
        for index, value in enumerate(self._model.feature_names):
            types[value] = self._model.feature_types[index]
        return types

    def get_missing_values(self):
        """Return mojo missing values"""

        return self._model.missing_values

    def get_prediction(self, d_frame):
        """Score and return predictions on a given dataset"""

        return self._model.predict(d_frame)


class ScorerError(Exception):
    """Base Scorer Error"""
    status_code = 500

class BadRequest(ScorerError):
    """Bad request"""
    status_code = 400


class ScorerAPI(Resource):

    def post(self):
        response_payload = request_handler(request)
        return response_payload


class PingAPI(Resource):

    def get(self):
        pass


api.add_resource(ScorerAPI, '/invocations')
api.add_resource(PingAPI, '/ping')


def request_handler(request):
    request_body = request.get_json()
    if request_body is None or len(request_body.keys()) == 0:
       raise BadRequest("Invalid request. Need a request body.")

    if 'fields' not in request_body.keys() or not isinstance(request_body['fields'], list):
       raise BadRequest("Cannot determine the request column fields")

    if 'rows' not in request_body.keys() or not isinstance(request_body['rows'], list):
       raise BadRequest("Cannot determine the request rows")

    scoring_result = score(request_body)
    return scoring_result

def score(request_body):
    mojo = MojoPipeline()

    # properly define types based on the request elements order
    tmp_frame = dt.Frame(
        [list(x) for x in list(zip(*request_body['rows']))],
        names=list(mojo.get_types().keys()),
        stypes=list(mojo.get_types().values())
    )

    d_frame = dt.fread(
        text=tmp_frame.to_csv(),
        columns=list(mojo.get_types().keys()),
        na_strings=mojo.get_missing_values()
    )

    result_frame = mojo.get_prediction(d_frame)
    combined_frame = dt.cbind(d_frame, result_frame)
    pandas_df = combined_frame.to_pandas()
    json_response = pandas_df.to_json(orient = 'records', date_format='iso')

    return json_response

if __name__ == '__main__':
    logger.info('==== Starting the H2O mojo-cpp scoring server =====')
    app.run(host='0.0.0.0', port=8080)
