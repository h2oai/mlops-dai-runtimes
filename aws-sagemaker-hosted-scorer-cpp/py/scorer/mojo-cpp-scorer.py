from flask import Flask, request
from flask_restful import Resource, Api
import logging
import json
import os
import threading
import daimojo.model
import datatable as dt

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


class PredictAPI(Resource):

    def post(self):
        request_body = request.get_json()
        res = predict(request_body)

        json_response = json.dumps(res)
        return json_response


class PingAPI(Resource):

    def get(self):
        pass


api.add_resource(PredictAPI, '/invocations')
api.add_resource(PingAPI, '/ping')


def predict(request_body):
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

    result = mojo.get_prediction(d_frame)

    return {
        'Scores': result.to_list()
    }


if __name__ == '__main__':
    logger.info('==== Starting the H2O mojo-cpp scoring server =====')
    app.run(host='0.0.0.0', port=8080)
