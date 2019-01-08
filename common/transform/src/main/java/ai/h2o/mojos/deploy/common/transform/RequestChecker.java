package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Checks that the request is of the correct form matching the corresponding mojo pipeline.
 */
public class RequestChecker {

    /**
     * Verify that the request is valid and matches the expected {@link MojoFrameMeta}.
     *
     * @throws ScoreRequestFormatException on any issues.
     */
    public void verify(ScoreRequest scoreRequest, MojoFrameMeta expectedMeta) throws ScoreRequestFormatException {
        String message = getProblemMessageOrNull(scoreRequest, expectedMeta);
        if (message == null) {
            return;
        }
        throw new ScoreRequestFormatException(message, getExampleRequest(expectedMeta));
    }

    private String getProblemMessageOrNull(ScoreRequest scoreRequest, MojoFrameMeta expectedMeta) {
        if (scoreRequest == null) {
            return "Request cannot be empty";
        }
        List<String> fields = scoreRequest.getFields();
        if (fields == null || fields.isEmpty()) {
            return "List of input fields cannot be empty";
        }
        List<Row> rows = scoreRequest.getRows();
        if (rows == null || rows.isEmpty()) {
            return "List of input data rows cannot be empty";
        }
        Set expFields = new HashSet<String>(asList(expectedMeta.getColumnNames()));
        if (!expFields.equals(new HashSet<String>(fields))) {
            return String.format("Input fields don't match the Mojo, expected %s actual %s",
                    Arrays.toString(expectedMeta.getColumnNames()), fields.toString());
        }
        int i = 0;
        for (Row row : scoreRequest.getRows()) {
            if (row.size() != fields.size()) {
                return String.format("Not enough elements in row %d (zero-indexed)", i);
            }
            i++;
        }
        return null;
    }

    private static ScoreRequest getExampleRequest(MojoFrameMeta expectedMeta) {
        ScoreRequest request = new ScoreRequest();
        request.setFields(asList(expectedMeta.getColumnNames()));
        Row row = new Row();
        for (MojoColumn.Type type : expectedMeta.getColumnTypes()) {
            row.add(getExampleValue(type));
        }
        request.addRowsItem(row);
        return request;
    }

    private static String getExampleValue(MojoColumn.Type type) {
        switch (type) {
            case Bool:
                return "true";
            case Int32:
            case Int64:
            case Float32:
            case Float64:
                return "0";
            case Str:
                return "text";
            case Time64:
                return "2018-01-01";
            default:
                return "";
        }
    }
}
