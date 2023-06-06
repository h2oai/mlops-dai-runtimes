package ai.h2o.mojos.deploy.local.rest.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScorerJsonLayout extends JsonLayout {
  public static enum RequestType {
    POST("POST"),
    GET("GET"),
    PATCH("PATCH"),
    PUT("PUT");

    private final String value;
    private RequestType(String value) {
      this.value = value;
    }
  }

  public static final String ENDPOINT = "Endpoint";
  public static final String ERROR = "Error";
  public static final String MESSAGE = "Message";
  public static final String MODEL_ID = "ModelId";
  public static final String NUM_ROWS = "NumberOfRows";
  public static final String REQUEST_TYPE = "RequestType";
  public static final String RESPONSE_CODE = "ResponseCode";
  public static final String SCORER_TYPE = "ScorerType";
  public static final String TIMESTAMP = "Timestamp";
  public static final String LOG_LEVEL = "LogLevel";

  public ScorerJsonLayout() {
    super();
  }

  @Override
  protected Map toJsonMap(ILoggingEvent event) {
    Map<String, Object> map = new LinkedHashMap<>();

    addTimestamp(TIMESTAMP, includeTimestamp, event.getTimeStamp(), map);
    add(LOG_LEVEL, includeLevel, String.valueOf(event.getLevel()), map);
    add(MESSAGE, includeFormattedMessage, event.getFormattedMessage(), map);
    addThrowableInfo(ERROR, includeException, event, map);
    add(SCORER_TYPE, containsMdc(event, SCORER_TYPE), getFromMdc(event, SCORER_TYPE), map);
    add(ENDPOINT, containsMdc(event, ENDPOINT), getFromMdc(event, ENDPOINT), map);
    add(REQUEST_TYPE, containsMdc(event, REQUEST_TYPE), getFromMdc(event, REQUEST_TYPE), map);
    add(MODEL_ID, containsMdc(event, MODEL_ID), getFromMdc(event, MODEL_ID), map);
    add(NUM_ROWS, containsMdc(event, NUM_ROWS), getFromMdc(event, NUM_ROWS), map);
    add(RESPONSE_CODE, containsMdc(event, RESPONSE_CODE), getFromMdc(event, RESPONSE_CODE), map);
    addCustomDataToJsonMap(map, event);
    return map;
  }

  private String getFromMdc(ILoggingEvent event, String field) {
    return event.getMDCPropertyMap().getOrDefault(field, null);
  }

  private boolean containsMdc(ILoggingEvent event, String field) {
    return event.getMDCPropertyMap().containsKey(field)
      && !event.getMDCPropertyMap().get(field).isEmpty();
  }
}
