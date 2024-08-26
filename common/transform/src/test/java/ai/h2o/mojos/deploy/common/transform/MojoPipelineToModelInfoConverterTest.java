package ai.h2o.mojos.deploy.common.transform;

import static com.google.common.truth.Truth.assertThat;

import ai.h2o.mojos.deploy.common.rest.model.DataField;
import ai.h2o.mojos.deploy.common.rest.model.DataField.DataTypeEnum;
import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.api.BasePipelineListener;
import ai.h2o.mojos.runtime.api.MojoColumnMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoColumn.Kind;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.frame.MojoFrameBuilder;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MojoPipelineToModelInfoConverterTest {
  private static String TEST_UUID = "TEST_UUID";
  private final MojoPipelineToModelInfoConverter converter = new MojoPipelineToModelInfoConverter();

  @Test
  void convertNoFields_succeeds() {
    // Given
    MojoPipeline pipeline =
        new DummyPipeline(TEST_UUID, MojoFrameMeta.getEmpty(), MojoFrameMeta.getEmpty());

    // When
    Model result = converter.apply(pipeline);

    // Then
    assertThat(result.getId()).isEqualTo(TEST_UUID);
    assertThat(result.getSchema().getInputFields()).isEmpty();
    assertThat(result.getSchema().getOutputFields()).isEmpty();
  }

  @Test
  void convertSingleInputField_succeeds() {
    // Given
    String[] inputNames = {"field1"};
    final List<MojoColumnMeta> columns =
        Collections.singletonList(MojoColumnMeta.newInput(inputNames[0], Type.Str));
    MojoPipeline pipeline =
        DummyPipeline.ofMeta(new MojoFrameMeta(columns), MojoFrameMeta.getEmpty());

    // When
    Model result = converter.apply(pipeline);

    // Then
    DataTypeEnum[] expectedInputTypes = {DataTypeEnum.STR};
    assertThat(result.getSchema().getInputFields())
        .containsExactlyElementsIn(toDataFields(inputNames, expectedInputTypes))
        .inOrder();
    assertThat(result.getSchema().getOutputFields()).isEmpty();
  }

  @Test
  void convertSingleOutputField_succeeds() {
    // Given
    String[] outputNames = {"field1"};
    final List<MojoColumnMeta> columns =
        Collections.singletonList(MojoColumnMeta.newOutput(outputNames[0], Type.Str));
    MojoPipeline pipeline =
        DummyPipeline.ofMeta(MojoFrameMeta.getEmpty(), new MojoFrameMeta(columns));

    // When
    Model result = converter.apply(pipeline);

    // Then
    DataTypeEnum[] expectedOutputTypes = {DataTypeEnum.STR};
    assertThat(result.getSchema().getInputFields()).isEmpty();
    assertThat(result.getSchema().getOutputFields())
        .containsExactlyElementsIn(toDataFields(outputNames, expectedOutputTypes))
        .inOrder();
  }

  @Test
  void convertMoreFields_succeeds() {
    // Given
    String[] inputNames = {"field1", "field2"};
    Type[] inputTypes = {Type.Str, Type.Str};
    String[] outputNames = {"field3", "field4"};
    Type[] outputTypes = {Type.Int64, Type.Int64};
    MojoPipeline pipeline =
        DummyPipeline.ofMeta(
            new MojoFrameMeta(MojoColumnMeta.toColumns(inputNames, inputTypes, Kind.Feature)),
            new MojoFrameMeta(MojoColumnMeta.toColumns(outputNames, outputTypes, Kind.Output)));

    // When
    Model result = converter.apply(pipeline);

    // Then
    DataTypeEnum[] expectedInputTypes = {DataTypeEnum.STR, DataTypeEnum.STR};
    DataTypeEnum[] expectedOutputTypes = {DataTypeEnum.INT64, DataTypeEnum.INT64};
    assertThat(result.getSchema().getInputFields())
        .containsExactlyElementsIn(toDataFields(inputNames, expectedInputTypes))
        .inOrder();
    assertThat(result.getSchema().getOutputFields())
        .containsExactlyElementsIn(toDataFields(outputNames, expectedOutputTypes))
        .inOrder();
  }

  @Test
  void convertAllTypes_succeeds() {
    // Given
    Type[] outputTypes = Type.values();
    String[] outputNames = Stream.of(Type.values()).map(Type::toString).toArray(String[]::new);
    final List<MojoColumnMeta> columns =
        MojoColumnMeta.toColumns(outputNames, outputTypes, Kind.Output);
    MojoPipeline pipeline =
        DummyPipeline.ofMeta(MojoFrameMeta.getEmpty(), new MojoFrameMeta(columns));

    // When
    Model result = converter.apply(pipeline);

    // Then
    DataTypeEnum[] expectedOutputTypes = DataTypeEnum.values();
    assertThat(result.getSchema().getInputFields()).isEmpty();
    // No ordering as it differ between the two enums.
    assertThat(result.getSchema().getOutputFields())
        .containsExactlyElementsIn(toDataFields(outputNames, expectedOutputTypes));
  }

  private static DataField[] toDataFields(String[] inputNames, DataTypeEnum[] inputTypes) {
    DataField[] result = new DataField[inputNames.length];
    for (int i = 0; i < inputNames.length; i++) {
      DataField dataField = new DataField();
      dataField.name(inputNames[i]);
      dataField.dataType(inputTypes[i]);
      result[i] = dataField;
    }
    return result;
  }

  /**
   * Dummy test {@link MojoPipeline} just to be able to test the transformation.
   */
  private static class DummyPipeline extends MojoPipeline {
    private final MojoFrameMeta inputMeta;
    private final MojoFrameMeta outputMeta;

    private DummyPipeline(String uuid, MojoFrameMeta inputMeta, MojoFrameMeta outputMeta) {
      super(uuid, null, null);
      this.inputMeta = inputMeta;
      this.outputMeta = outputMeta;
    }

    static DummyPipeline ofMeta(MojoFrameMeta inputMeta, MojoFrameMeta outputMeta) {
      return new DummyPipeline(TEST_UUID, inputMeta, outputMeta);
    }

    @Override
    public MojoFrameMeta getInputMeta() {
      return inputMeta;
    }

    @Override
    public MojoFrameMeta getOutputMeta() {
      return outputMeta;
    }

    @Override
    public MojoFrameBuilder getOutputFrameBuilder(MojoFrameBuilder inputFrameBuilder) {
      return null;
    }

    @Override
    protected MojoFrameBuilder getFrameBuilder(MojoColumn.Kind kind) {
      throw new AssertionError("Not supported by test DummyPipeline.");
    }

    @Override
    protected MojoFrameMeta getMeta(MojoColumn.Kind kind) {
      throw new AssertionError("Not supported by test DummyPipeline.");
    }

    @Override
    public MojoFrame transform(MojoFrame inputFrame, MojoFrame outputFrame) {
      throw new AssertionError("Not supported by test DummyPipeline.");
    }

    @Override
    public void setShapPredictContrib(boolean enable) {
      throw new AssertionError("Not supported by test DummyPipeline.");
    }

    @Override
    public void setShapPredictContribOriginal(boolean enable) {
      throw new AssertionError("Not supported by test DummyPipeline.");
    }

    @Override
    public void setListener(BasePipelineListener listener) {
      throw new AssertionError("Not supported by test DummyPipeline.");
    }

    @Override
    public void printPipelineInfo(PrintStream out) {
    }
  }
}
