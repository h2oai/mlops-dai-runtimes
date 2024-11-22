package ai.h2o.mojos.deploy.local.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import ai.h2o.mojos.deploy.common.rest.model.Model;
import ai.h2o.mojos.deploy.common.rest.model.ModelSchema;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ReadyzApiControllerTest {

  @Mock private MojoScorer scorer;

  @InjectMocks private ReadyzApiController controller;

  @Test
  void getReadyz_WhenModelInfoSucceeds_ReturnsOk() {
    Model model = new Model();
    model.setSchema(new ModelSchema());
    when(scorer.getModelInfo()).thenReturn(model);

    ResponseEntity<String> response = controller.getReadyz();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Ready", response.getBody());
  }

  @Test
  void getReadyz_WhenModelInfoFails_ReturnsError() {
    when(scorer.getModelInfo()).thenThrow(new RuntimeException("Test error"));

    ResponseEntity<String> response = controller.getReadyz();

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Not ready", response.getBody());
  }
}
