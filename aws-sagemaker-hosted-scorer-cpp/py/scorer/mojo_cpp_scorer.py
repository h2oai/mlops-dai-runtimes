import logging
import os
import threading
import time

import daimojo.model
import datatable as dt
from flask import Flask, request
from flask_restful import Resource, Api

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
        self._set_omp_threads()
        mojo_file_path = os.getenv('MOJO_FILE_PATH')
        self._model = daimojo.model(mojo_file_path)

    def get_id(self):
        return self._model.uuid

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
        self._set_omp_threads()
        return self._model.predict(d_frame)

    @staticmethod
    def _set_omp_threads():
        os.environ['OMP_NUM_THREADS'] = str(1)


class ScorerError(Exception):
    """Base Scorer Error"""
    status_code = 500


class BadRequest(ScorerError):
    """Bad request"""
    status_code = 400


def score(request_body, timeit=False):
    if request_body is None or len(request_body.keys()) == 0:
        raise BadRequest("Invalid request. Need a request body.")

    if 'fields' not in request_body.keys() or not isinstance(request_body['fields'], list):
        raise BadRequest("Cannot determine the request column fields")

    if 'rows' not in request_body.keys() or not isinstance(request_body['rows'], list):
        raise BadRequest("Cannot determine the request rows")

    mojo = MojoPipeline()

    # properly define types based on the request elements order
    d_frame = dt.Frame(
        [list(x) for x in list(zip(*request_body['rows']))],
        names=list(mojo.get_types().keys()),
        stypes=list(mojo.get_types().values())
    )

    start = time.time() if timeit else 0
    result_frame = mojo.get_prediction(d_frame)
    delta = time.time() - start if timeit else 0
    request_id = mojo.get_id()

    # check whether 'includeFieldsInOutput' comes with request body and combined them with scored result
    if 'includeFieldsInOutput' in request_body:
        include_fields = list(request_body['includeFieldsInOutput'])
        if len(include_fields) > 0:
            result_frame = get_combined_frame(d_frame, result_frame, include_fields)

    ret = {
        'id': request_id,
        'fields': result_frame.names,
        'score': result_frame.to_list()
    }
    if timeit:
        ret['time'] = delta

    return ret


def get_combined_frame(input_frame, result_frame, include_on_out_fields):
    combined_frame = dt.Frame()

    for include_column in include_on_out_fields:
        combined_frame = dt.cbind(combined_frame, input_frame[include_column])

    combined_frame = dt.cbind(combined_frame, result_frame)
    return combined_frame


class ScorerAPI(Resource):
    def post(self):
        request_body = request.get_json()
        try:
            scoring_result = score(request_body, timeit='timeit' in request.args.keys())
        except ScorerError as e:
            return {'message': str(e)}, e.status_code
        except Exception as exc:
            return {'message': str(exc)}, 500
        return scoring_result, 200


class PingAPI(Resource):

    def get(self):
        pass


api.add_resource(ScorerAPI, '/invocations')
api.add_resource(PingAPI, '/ping')

if __name__ == '__main__':
    logger.info('==== Starting the H2O mojo-cpp scoring server =====')
    app.run(host='0.0.0.0', port=8080)
