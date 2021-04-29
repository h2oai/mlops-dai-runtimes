package ai.h2o.mojos.deploy.common.transform.unified;

import ai.h2o.mojos.deploy.common.rest.unified.model.DataField;
import ai.h2o.mojos.deploy.common.rest.unified.model.Model;
import ai.h2o.mojos.deploy.common.rest.unified.model.ModelSchema;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoColumn.Type;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Converts the {@link MojoPipeline} schema to the REST API Model information {@link Model}. */
public class MojoPipelineToModelInfoConverter implements Function<MojoPipeline, Model> {

  @Override
  public Model apply(MojoPipeline pipeline) {
    Model model = new Model();
    model.setId(pipeline.getUuid());
    model.setSchema(extractSchema(pipeline));
    return model;
  }

  private static ModelSchema extractSchema(MojoPipeline pipeline) {
    ModelSchema schema = new ModelSchema();
    schema.inputFields(extractFields(pipeline.getInputMeta()));
    schema.outputFields(extractFields(pipeline.getOutputMeta()));
    return schema;
  }

  private static List<DataField> extractFields(MojoFrameMeta meta) {
    List<DataField> fields = new ArrayList<>(meta.size());
    for (int i = 0; i < meta.size(); i++) {
      DataField dataField = new DataField();
      dataField.name(meta.getColumnName(i));
      dataField.dataType(mojoTypeToDataType(meta.getColumnType(i)));
      fields.add(dataField);
    }
    return fields;
  }

  private static DataField.DataTypeEnum mojoTypeToDataType(Type columnType) {
    return DataField.DataTypeEnum.fromValue(columnType.toString());
  }
}
