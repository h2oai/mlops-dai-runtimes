resource "aws_api_gateway_rest_api" "scorer_api" {
  name = "${var.lambda_id}_api"
  description = "H2O Driverless AI Mojo Scorer API (${var.lambda_id})"
}

resource "aws_api_gateway_resource" "proxy_resource" {
  rest_api_id = "${aws_api_gateway_rest_api.scorer_api.id}"
  parent_id = "${aws_api_gateway_rest_api.scorer_api.root_resource_id}"
  path_part = "score"
}

resource "aws_api_gateway_method" "proxy_method" {
  rest_api_id = "${aws_api_gateway_rest_api.scorer_api.id}"
  resource_id = "${aws_api_gateway_resource.proxy_resource.id}"
  http_method = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "scorer_integration" {
  rest_api_id = "${aws_api_gateway_rest_api.scorer_api.id}"
  resource_id = "${aws_api_gateway_method.proxy_method.resource_id}"
  http_method = "${aws_api_gateway_method.proxy_method.http_method}"

  integration_http_method = "POST"
  type = "AWS_PROXY"
  uri = "${aws_lambda_function.scorer_lambda.invoke_arn}"
}

resource "aws_api_gateway_deployment" "scorer_api_deployment" {
  depends_on = [
    "aws_api_gateway_integration.scorer_integration"
  ]

  rest_api_id = "${aws_api_gateway_rest_api.scorer_api.id}"
  stage_name = "test"
}

resource "aws_lambda_permission" "apigw" {
  statement_id = "AllowExecutionFromAPIGateway"
  action = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.scorer_lambda.arn}"
  principal = "apigateway.amazonaws.com"

  # The /*/*/* part allows invocation from any stage, method and resource path
  # within API Gateway REST API.
  source_arn = "${aws_api_gateway_rest_api.scorer_api.execution_arn}/*/*/*"
}

output "base_url" {
  value = "${aws_api_gateway_deployment.scorer_api_deployment.invoke_url}"
}

