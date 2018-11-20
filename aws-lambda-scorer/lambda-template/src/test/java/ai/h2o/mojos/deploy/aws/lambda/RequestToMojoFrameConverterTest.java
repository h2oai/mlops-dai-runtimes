package ai.h2o.mojos.deploy.aws.lambda;

import ai.h2o.mojos.deploy.aws.lambda.model.Row;
import ai.h2o.mojos.deploy.aws.lambda.model.ScoreRequest;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

class RequestToMojoFrameConverterTest {
    private final RequestToMojoFrameConverter converter = new RequestToMojoFrameConverter();

    @Test
    void convertEmptyRowsRequest_succeeds() {
        // Given
        ScoreRequest request = new ScoreRequest();

        // When
        MojoFrame result = converter.apply(request, MojoFrameMeta.getEmpty());

        // Then
        assertThat(result.getNcols()).isEqualTo(0);
        assertThat(result.getNrows()).isEqualTo(0);
    }

    @Test
    void convertSingleFieldRequest_succeeds() {
        // Given
        ScoreRequest request = new ScoreRequest();
        request.addFieldsItem("field1");
        request.addRowsItem(asRow("value"));

        // When
        MojoFrame result = converter.apply(request,
                new MojoFrameMeta(
                        new String[]{"field1"},
                        new Type[]{Type.Str}));

        // Then
        assertThat(result.getNcols()).isEqualTo(1);
        assertThat(result.getNrows()).isEqualTo(1);
        assertThat(result.getColumnNames()).asList().containsExactly("field1");
        assertThat(result.getColumn(0).getDataAsStrings()).asList().containsExactly("value");
    }

    @Test
    void convertMoreRowsRequest_succeeds() {
        // Given
        String[] values = {"value1", "value2", "value3"};
        ScoreRequest request = new ScoreRequest();
        request.addFieldsItem("field1");
        request.rows(
                Stream.of(values).map(RequestToMojoFrameConverterTest::asRow).collect(toList()));

        // When
        MojoFrame result = converter.apply(request,
                new MojoFrameMeta(
                        new String[]{"field1"},
                        new Type[]{Type.Str}));

        // Then
        assertThat(result.getNcols()).isEqualTo(1);
        assertThat(result.getNrows()).isEqualTo(3);
        assertThat(result.getColumnNames()).asList().containsExactly("field1");
        assertThat(result.getColumn(0).getDataAsStrings()).isEqualTo(values);
    }

    @Test
    void convertMoreTypesRequest_succeeds() {
        // Given
        Type[] types = {Type.Str, Type.Float32, Type.Float64, Type.Bool, Type.Int32, Type.Int64};
        List<String> fields = Stream.of(types).map(Object::toString).collect(toList());
        String[] values = {"str", "1.1", "2.2", "1", "123", "123456789"};
        ScoreRequest request = new ScoreRequest();
        request.setFields(fields);
        request.addRowsItem(asRow(values));

        // When
        MojoFrame result = converter.apply(request,
                new MojoFrameMeta(fields.toArray(new String[0]), types));

        // Then
        assertThat(result.getNcols()).isEqualTo(types.length);
        assertThat(result.getNrows()).isEqualTo(1);
        assertThat(result.getColumnNames()).asList().containsExactlyElementsIn(fields);
        for (int col = 0; col < types.length; col++) {
            assertThat(result.getColumn(col).getDataAsStrings()[0]).isEqualTo(values[col]);
        }
    }

    private static Row asRow(String... values) {
        Row row = new Row();
        row.ensureCapacity(values.length);
        row.addAll(asList(values));
        return row;
    }
}