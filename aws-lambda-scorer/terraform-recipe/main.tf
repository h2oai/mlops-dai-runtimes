variable "access_key" {}
variable "secret_key" {}
variable "region" {
  default = "us-east-1"
}
variable "lambda_id" {
  default = "h2oai_dai_lambda"
}
variable "lambda_zip_path" {
  default = "../lambda-template/build/distributions/lambda-template.zip"
}

provider "aws" {
  access_key = "${var.access_key}"
  secret_key = "${var.secret_key}"
  region = "${var.region}"
}

resource "aws_lambda_function" "scorer_lambda" {
  function_name = "${var.lambda_id}_function"
  description = "H2O Driverless AI Mojo Scorer"
  filename = "${var.lambda_zip_path}"
  handler = "ai.h2o.deploy.aws.lambda.MojoScorer::handleRequest"
  source_code_hash = "${base64sha256(file(var.lambda_zip_path))}"
  role = "${aws_iam_role.scorer_lambda_iam_role.arn}"
  runtime = "java8"

  // Increase resource constraints from the defaults of 3s and 128MB.
  timeout = 120
  memory_size = 1024

  environment {
    variables = {
      // TODO(osery): Pass in a location of the mojo in S3.
    }
  }
}

# IAM role which dictates what other AWS services the lambda function may access.
resource "aws_iam_role" "scorer_lambda_iam_role" {
  name = "${var.lambda_id}_iam_role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}
