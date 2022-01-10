FROM python:3.8

RUN apt-get update

RUN pip3 install https://s3.amazonaws.com/artifacts.h2o.ai/releases/ai/h2o/daimojo/2.7.5/x86_64-centos7/daimojo-2.7.5-cp38-cp38-linux_x86_64.whl

RUN pip3 install datatable

RUN pip3 install flask-restful

ENV DRIVERLESS_AI_LICENSE_FILE='/opt/ml/model/license.sig'

ENV MOJO_FILE_PATH='/opt/ml/model/pipeline.mojo'

RUN mkdir -p /opt/ml/code

COPY py/scorer/mojo-cpp-scorer.py /opt/ml/code

WORKDIR /opt/ml/code

ENTRYPOINT ["python", "mojo-cpp-scorer.py"]
