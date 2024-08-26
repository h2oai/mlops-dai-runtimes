package ai.h2o.mojos.deploy.local.rest.converter;

import ai.h2o.mojos.deploy.common.rest.model.ScoreMediaRequest;
import com.google.gson.Gson;
import org.springframework.core.convert.converter.Converter;

public class ScoreMediaRequestConverter implements Converter<String, ScoreMediaRequest> {

  @Override
  public ScoreMediaRequest convert(String input) {
    return new Gson().fromJson(input, ScoreMediaRequest.class);
  }
}
