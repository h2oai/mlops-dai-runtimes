package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.deploy.common.rest.model.ScoreResponse;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import ai.h2o.mojos.runtime.frame.MojoRowBuilder;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

class MojoFrameToResponseConverterTest {
    private final MojoFrameToResponseConverter converter = new MojoFrameToResponseConverter();

    @Test
    void convertEmptyRowsResponse_succeeds() {
        // Given
        ScoreRequest scoreRequest = new ScoreRequest();
        MojoFrame mojoFrame = new MojoFrameBuilder(MojoFrameMeta.getEmpty()).toMojoFrame();

        // When
        ScoreResponse result = converter.apply(mojoFrame, scoreRequest);

        // Then
        assertThat(result.getScore()).isEmpty();
    }

    @Test
    void convertSingleFieldResponse_succeeds() {
        // Given
        String[] fields = {"field"};
        Type[] types = {Type.Str};
        String[][] values = {{"value"}};
        ScoreRequest scoreRequest = new ScoreRequest();

        // When
        ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

        // Then
        assertThat(result.getScore()).containsExactly(asRow("value"));
    }

    @Test
    void convertIncludesOneField_succeeds() {
        // Given
        String[] fields = {"outputField"};
        Type[] types = {Type.Str};
        String[][] values = {{"outputValue"}};
        ScoreRequest scoreRequest = new ScoreRequest();
        scoreRequest.addFieldsItem("inputField");
        scoreRequest.addIncludeFieldsInOutputItem("inputField");
        scoreRequest.addRowsItem(asRow("inputValue"));

        // When
        ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

        // Then
        assertThat(result.getScore()).containsExactly(asRow("inputValue", "outputValue"));
    }

    @Test
    void convertIncludesSomeFields_succeeds() {
        // Given
        String[] fields = {"outputField1", "outputField2"};
        Type[] types = {Type.Str, Type.Str};
        String[][] values = {{"outputValue1", "outputValue2"}};
        ScoreRequest scoreRequest = new ScoreRequest();
        scoreRequest.setFields(asList("inputField1", "inputField2", "inputField3"));
        scoreRequest.setIncludeFieldsInOutput(asList("inputField1", "inputField3"));
        scoreRequest.addRowsItem(asRow("inputValue1", "omittedValue", "inputValue3"));

        // When
        ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

        // Then
        assertThat(result.getScore()).containsExactly(
                asRow("inputValue1", "inputValue3", "outputValue1", "outputValue2"));
    }

    @Test
    void convertMoreRowsResponse_succeeds() {
        // Given
        String[] fields = {"field"};
        Type[] types = {Type.Str};
        String[][] values = {{"value1"}, {"value2"}, {"value3"}};
        ScoreRequest scoreRequest = new ScoreRequest();

        // When
        ScoreResponse result = converter.apply(buildMojoFrame(fields, types, values), scoreRequest);

        // Then
        assertThat(result.getScore()).containsExactly(Stream.of(values).map(MojoFrameToResponseConverterTest::asRow).toArray());
    }

    @Test
    void convertMoreTypesResponse_succeeds() {
        // Given
        Type[] types = {Type.Str, Type.Float32, Type.Float64, Type.Bool, Type.Int32, Type.Int64};
        String[][] values = {{"str", "1.1", "2.2", "1", "123", "123456789"}};
        ScoreRequest scoreRequest = new ScoreRequest();

        // When
        ScoreResponse result = converter.apply(
                buildMojoFrame(Stream.of(types).map(Object::toString).toArray(String[]::new), types, values),
                scoreRequest);

        // Then
        assertThat(result.getScore()).containsExactly(Stream.of(values).map(MojoFrameToResponseConverterTest::asRow).toArray());
    }

    static private MojoFrame buildMojoFrame(String[] fields, Type[] types, String[][] values) {
        MojoFrameMeta meta = new MojoFrameMeta(fields, types);
        MojoFrameBuilder frameBuilder = new MojoFrameBuilder(meta);
        for (String[] row : values) {
            MojoRowBuilder rowBuilder = frameBuilder.getMojoRowBuilder();
            int col = 0;
            for (String value : row) {
                rowBuilder.setValue(col++, value);
            }
            frameBuilder.addRow(rowBuilder);
        }
        return frameBuilder.toMojoFrame();
    }

    private static Row asRow(String... values) {
        Row row = new Row();
        row.ensureCapacity(values.length);
        row.addAll(asList(values));
        return row;
    }
}