from pyspark.sql import SparkSession
import argparse

from pyspark.sql.types import StringType,FloatType
from pyspark.sql.functions import udf

def main():
    spark = SparkSession.builder.appName('MOJO Pipeline Transformer').getOrCreate()
    spark.sparkContext.setLogLevel('WARN')

    parser = argparse.ArgumentParser()
    parser.add_argument("--mojo", type=str, required=True, help="Location of .mojo file, e.g., 'file:///tmp/pipeline.mojo'")
    parser.add_argument("--input", type=str, required=True, help="Location of input file, e.g., 'file:///tmp/example.csv'")
    parser.add_argument("--output", type=str, required=False, help="Location of output file, if not specified output is printed to console")
    parser.add_argument("--format", type=str, required=False, default='csv', help="Format of output file, if not specified format is csv")
    parser.add_argument("--nthreads", type=int, required=False, default=1, help="Number of threads to use, defaults to 1")

    args = parser.parse_args()

    # Load MOJO2 pipeline
    from pysparkling.ml import H2OMOJOPipelineModel
    mojo = H2OMOJOPipelineModel.create_from_mojo(args.mojo)
    mojo.set_named_mojo_output_columns(True)

    print("=============== In/Out ==============")
    print("Inputs : {}".format(mojo.get_input_names()))
    print("Outputs: {}".format(mojo.get_output_names()))
    print("=====================================")

    # Load data
    example_df = spark.read.csv(args.input, header=True).repartition(args.nthreads)
    prediction_df = mojo.transform(example_df)

    # Flatenize prediction column to allow export to file/console
    if args.format == 'parquet':
        flat_fce = lambda x: str(x)
    else:
        flat_fce = lambda x: str(x) # TODO
    flat_udf = udf(flat_fce, StringType())
    output_df = prediction_df.withColumn('prediction', flat_udf(prediction_df.prediction))
    if args.output:
        output_df.write.format(args.format).mode('overwrite').option("header", "true").save(args.output)
        print("Predictions written to: {}".format(args.output))
    else:
        output_df.show(10, False)

if __name__ == '__main__':
    main()
