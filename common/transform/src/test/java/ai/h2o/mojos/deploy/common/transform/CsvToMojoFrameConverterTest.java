package ai.h2o.mojos.deploy.common.transform;

import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvToMojoFrameConverterTest {
    private final CsvToMojoFrameConverter converter = new CsvToMojoFrameConverter();

    @Test
    void convertEmptyFile_fails() throws IOException {
        // Given
        InputStream csv = toCsvStream("");

        // When & Then
        assertThrows(IOException.class, () -> converter.apply(csv, MojoFrameMeta.getEmpty()));
    }

    @Test
    void convertColumnCountsMismatch_fails() throws IOException {
        // Given
        InputStream csv = toCsvStream("Field1,Field2,Field3");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> converter.apply(csv, MojoFrameMeta.getEmpty()));
    }

    @Test
    void convertOnlyHeaders_succeeds() throws IOException {
        // Given
        String[] names = {"Field1", "Field2", "Field3"};
        Type[] types = {Type.Str, Type.Str, Type.Str};
        InputStream csv = toCsvStream("Field1,Field2,Field3");

        // When
        MojoFrame result = converter.apply(csv, new MojoFrameMeta(names, types));

        // Then
        assertThat(result.getNcols()).isEqualTo(3);
        assertThat(result.getNrows()).isEqualTo(0);
    }

    @Test
    void convertWithData_succeeds() throws IOException {
        // Given
        String[] names = {"Field1", "Field2"};
        Type[] types = {Type.Str, Type.Int64};
        InputStream csv = toCsvStream("Field1,Field2", "value1,1", "value2,2");

        // When
        MojoFrame result = converter.apply(csv, new MojoFrameMeta(names, types));

        // Then
        assertThat(result.getColumnNames()).isEqualTo(names);
        assertThat(result.getColumnTypes()).isEqualTo(types);
        String[] expectedStrValues = {"value1", "value2"};
        String[] expectedInt64Values = {"1", "2"};
        assertThat(result.getColumn(0).getDataAsStrings()).isEqualTo(expectedStrValues);
        assertThat(result.getColumn(1).getDataAsStrings()).isEqualTo(expectedInt64Values);
    }

    @Test
    void convertWithColumnReordering_succeeds() throws IOException {
        // Given
        String[] names = {"Field1", "Field2"};
        Type[] types = {Type.Str, Type.Int64};
        InputStream csv = toCsvStream("Field2,Field1", "1,value1", "2,value2");

        // When
        MojoFrame result = converter.apply(csv, new MojoFrameMeta(names, types));

        // Then
        assertThat(result.getColumnNames()).isEqualTo(names);
        assertThat(result.getColumnTypes()).isEqualTo(types);
        String[] expectedStrValues = {"value1", "value2"};
        String[] expectedInt64Values = {"1", "2"};
        assertThat(result.getColumn(0).getDataAsStrings()).isEqualTo(expectedStrValues);
        assertThat(result.getColumn(1).getDataAsStrings()).isEqualTo(expectedInt64Values);
    }

    @Test
    void convertMoreTypes_succeeds() throws IOException {
        // Given
        Type[] types = {Type.Str, Type.Int32, Type.Int64, Type.Float32, Type.Float64, Type.Bool};
        String[] names = Stream.of(types).map(Object::toString).toArray(String[]::new);
        InputStream csv = toCsvStream(String.join(",", names), "test,1,2,1.5,2.5,false");

        // When
        MojoFrame result = converter.apply(csv, new MojoFrameMeta(names, types));

        // Then
        assertThat(result.getColumnNames()).isEqualTo(names);
        assertThat(result.getColumnTypes()).isEqualTo(types);
        String[] expectedValues = {"test", "1", "2", "1.5", "2.5", "0"};
        String[] actualValues = IntStream.range(0, types.length).mapToObj(
                i -> result.getColumn(i).getDataAsStrings()[0]).toArray(String[]::new);
        assertThat(actualValues).isEqualTo(expectedValues);
    }

    static private InputStream toCsvStream(String... rows) {
        return new ByteArrayInputStream(String.join("\n", rows).getBytes());
    }
}
