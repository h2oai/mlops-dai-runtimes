FROM python:3.8

RUN apt-get -y update && pip3 install datatable \
    flask-restful \
    https://s3.amazonaws.com/artifacts.h2o.ai/releases/ai/h2o/daimojo/2.7.5/x86_64-centos7/daimojo-2.7.5-cp38-cp38-linux_x86_64.whl

ENV DRIVERLESS_AI_LICENSE_FILE='/opt/ml/model/license.sig' \
    MOJO_FILE_PATH='/opt/ml/model/pipeline.mojo'

RUN mkdir -p /opt/ml/code

COPY py/scorer/mojo_cpp_scorer.py /opt/ml/code

WORKDIR /opt/ml/code

ENTRYPOINT ["python", "mojo_cpp_scorer.py"]
