package ai.h2o.mojos.deploy.common.transform;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import ai.h2o.mojos.deploy.common.rest.model.Row;
import ai.h2o.mojos.deploy.common.rest.model.ScoreRequest;
import ai.h2o.mojos.runtime.api.MojoColumnMeta;
import ai.h2o.mojos.runtime.frame.MojoColumn;
import ai.h2o.mojos.runtime.frame.MojoFrameMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
class RequestCheckerTest {
  private final ScoreRequest exampleRequest = new ScoreRequest();
  @Mock private SampleRequestBuilder sampleRequestBuilder;
  @InjectMocks private RequestChecker checker;

  MockitoSession mockito;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    mockito = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.STRICT_STUBS)
            .startMocking();
  }

  @After
  public void tearDown() {
    mockito.finishMocking();
  }

  @Test
  void verifyValidRequest_succeeds() throws ScoreRequestFormatException {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    MojoFrameMeta expectedMeta =
        new MojoFrameMeta(
            Collections.singletonList(MojoColumnMeta.newOutput("field1", MojoColumn.Type.Str)));

    // When
    checker.verify(request, expectedMeta);

    // Then all ok
  }

  @Test
  void verifyNullRequest_throws() {
    // Given
    ScoreRequest request = null;
    MojoFrameMeta expectedMeta = MojoFrameMeta.getEmpty();
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("empty");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  @Test
  void verifyEmptyRequest_throws() {
    // Given
    ScoreRequest request = new ScoreRequest();
    MojoFrameMeta expectedMeta = MojoFrameMeta.getEmpty();
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("empty");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  @Test
  void verifyEmptyFieldsRequest_throws() {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addRowsItem(toRow("text"));
    MojoFrameMeta expectedMeta =
        new MojoFrameMeta(
            Collections.singletonList(MojoColumnMeta.newOutput("field1", MojoColumn.Type.Str)));
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("fields cannot be empty");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  @Test
  void verifyEmptyRowsRequest_throws() {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    MojoFrameMeta expectedMeta =
        new MojoFrameMeta(
            Collections.singletonList(MojoColumnMeta.newOutput("field1", MojoColumn.Type.Str)));
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("rows cannot be empty");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  @Test
  void verifyMismatchedFieldsRequest_throws() {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("a_fields");
    request.addRowsItem(toRow("text"));
    MojoFrameMeta expectedMeta =
        new MojoFrameMeta(
            Collections.singletonList(
                MojoColumnMeta.newOutput("different_fields", MojoColumn.Type.Str)));
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("fields don't contain all");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  @Test
  void verifyTooFewFieldsRequest_throws() {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    final List<MojoColumnMeta> columns = new ArrayList<>();
    columns.add(MojoColumnMeta.newOutput("field1", MojoColumn.Type.Str));
    columns.add(MojoColumnMeta.newOutput("field2", MojoColumn.Type.Str));
    final MojoFrameMeta expectedMeta = new MojoFrameMeta(columns);
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("fields don't contain all");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  @Test
  void verifyExtraFieldsRequest_succeeds() throws ScoreRequestFormatException {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addFieldsItem("field2");
    request.addRowsItem(toRow("text1", "text2"));
    MojoFrameMeta expectedMeta =
        new MojoFrameMeta(
            Collections.singletonList(MojoColumnMeta.newOutput("field1", MojoColumn.Type.Str)));

    // When
    checker.verify(request, expectedMeta);

    // Then all ok
  }

  @Test
  void verifyMismatchedRowsRequest_throws() {
    // Given
    ScoreRequest request = new ScoreRequest();
    request.addFieldsItem("field1");
    request.addRowsItem(toRow("text"));
    request.addRowsItem(toRow("text", "additional text"));
    MojoFrameMeta expectedMeta =
        new MojoFrameMeta(
            Collections.singletonList(MojoColumnMeta.newOutput("field1", MojoColumn.Type.Str)));
    given(sampleRequestBuilder.build(any())).willReturn(exampleRequest);

    // When & then
    ScoreRequestFormatException exception =
        assertThrows(
            ScoreRequestFormatException.class, () -> checker.verify(request, expectedMeta));
    assertThat(exception.getMessage()).contains("row 1");
    assertThat(exception.getExampleRequest()).isSameAs(exampleRequest);
  }

  private static Row toRow(String... values) {
    Row row = new Row();
    row.addAll(Arrays.asList(values));
    return row;
  }
}
