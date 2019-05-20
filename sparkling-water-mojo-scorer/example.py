from pyspark.sql import SparkSession
import argparse

from pyspark.sql.types import StringType,FloatType
from pyspark.sql.functions import monotonically_increasing_id

def main():
    spark = SparkSession.builder.appName('MOJO Pipeline Transformer').getOrCreate()
    spark.sparkContext.setLogLevel('WARN')

    parser = argparse.ArgumentParser()
    parser.add_argument("--mojo", type=str, required=True, help="Location of .mojo file, e.g., 'file:///tmp/pipeline.mojo'")
    parser.add_argument("--input", type=str, required=True, help="Location of input file, e.g., 'file:///tmp/example.csv'")
    parser.add_argument("--output", type=str, required=False, help="Directory location of output files, if not specified output is printed to console")
    parser.add_argument("--format", type=str, required=False, default='csv', help="Format of output file, if not specified format is csv")
    parser.add_argument("--format_options", type=dict, required=False, default={'header': True}, help="Input options for the underlying data source")
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
    example_df = spark.read.\
            format(args.format).\
            options(**args.format_options).\
            load(args.input).\
            repartition(args.nthreads).\
            withColumn('', monotonically_increasing_id())
    prediction_df = mojo.transform(example_df)
    output_df = prediction_df.select(['','prediction.*'])

    # Write ouput
    if args.output:
        output_df.write.format(args.format).mode('overwrite').option("header", "true").save(args.output)
        print("Predictions written to: {}".format(args.output))
    else:
        output_df.show(10, False)

if __name__ == '__main__':
    main()
