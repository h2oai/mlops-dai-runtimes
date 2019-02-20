package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestCheckerTest {
    private final RequestChecker checker = new RequestChecker();

    @Test
    void verifyValidRequest_succeeds() throws ScoreRequestFormatException {
        // Given
        ScoreRequest request = new ScoreRequest();
        request.addFieldsItem("field1");
        request.addRowsItem(toRow("text"));
        MojoFrameMeta expectedMeta = new MojoFrameMeta(
                new String[]{"field1"},
                new MojoColumn.Type[]{MojoColumn.Type.Str});

        // When
        checker.verify(request, expectedMeta);

        // Then all ok
    }

    @Test
    void verifyNullRequest_throws() {
        // Given
        ScoreRequest request = null;
        MojoFrameMeta expectedMeta = MojoFrameMeta.getEmpty();

        // When & then
        ScoreRequestFormatException exception =
                assertThrows(ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
        assertThat(exception.getMessage()).contains("empty");
    }

    @Test
    void verifyEmptyRequest_throws() {
        // Given
        ScoreRequest request = new ScoreRequest();
        MojoFrameMeta expectedMeta = MojoFrameMeta.getEmpty();

        // When & then
        ScoreRequestFormatException exception =
                assertThrows(ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
        assertThat(exception.getMessage()).contains("empty");
    }

    @Test
    void verifyEmptyFieldsRequest_throws() {
        // Given
        ScoreRequest request = new ScoreRequest();
        request.addRowsItem(toRow("text"));
        MojoFrameMeta expectedMeta = new MojoFrameMeta(
                new String[]{"field1"},
                new MojoColumn.Type[]{MojoColumn.Type.Str});

        // When & then
        ScoreRequestFormatException exception =
                assertThrows(ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
        assertThat(exception.getMessage()).contains("fields cannot be empty");
    }

    @Test
    void verifyEmptyRowsRequest_throws() {
        // Given
        ScoreRequest request = new ScoreRequest();
        request.addFieldsItem("field1");
        MojoFrameMeta expectedMeta = new MojoFrameMeta(
                new String[]{"field1"},
                new MojoColumn.Type[]{MojoColumn.Type.Str});

        // When & then
        ScoreRequestFormatException exception =
                assertThrows(ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
        assertThat(exception.getMessage()).contains("rows cannot be empty");
    }

    @Test
    void verifyMismatchedFieldsRequest_throws() {
        // Given
        ScoreRequest request = new ScoreRequest();
        request.addFieldsItem("a_fields");
        request.addRowsItem(toRow("text"));
        MojoFrameMeta expectedMeta = new MojoFrameMeta(
                new String[]{"different_fields"},
                new MojoColumn.Type[]{MojoColumn.Type.Str});

        // When & then
        ScoreRequestFormatException exception =
                assertThrows(ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
        assertThat(exception.getMessage()).contains("fields don't match");
    }

    @Test
    void verifyMismatchedRowsRequest_throws() {
        // Given
        ScoreRequest request = new ScoreRequest();
        request.addFieldsItem("field1");
        request.addRowsItem(toRow("text"));
        request.addRowsItem(toRow("text", "additional text"));
        MojoFrameMeta expectedMeta = new MojoFrameMeta(
                new String[]{"field1"},
                new MojoColumn.Type[]{MojoColumn.Type.Str});

        // When & then
        ScoreRequestFormatException exception =
                assertThrows(ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
        assertThat(exception.getMessage()).contains("row 1");
    }

    private static Row toRow(String... values) {
        Row row = new Row();
        row.addAll(Arrays.asList(values));
        return row;
    }
}
