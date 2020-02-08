package ai.h2o.mojos.deploy.gcp.cloud.run.controller;

import ai.h2o.mojos.deploy.common.rest.api.ModelApi;
import ai.h2o.mojos.deploy.common.transform.MojoScorer;
import ai.h2o.mojos.deploy.common.transform.SampleRequestBuilder;
import ai.h2o.mojos.deploy.local.rest.controller.ModelsApiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ModelsApiControllerGcp extends ModelsApiController {

  /**
   * Simple Api controller. Inherits from {@link ModelApi}, which controls global, expected request
   * mappings for the rest service.
   *
   * @param scorer {@link MojoScorer} initialized class containing loaded mojo, and mojo interaction
   *     methods
   * @param sampleRequestBuilder {@link SampleRequestBuilder} initialized class, for generating
   */
  @Autowired
  public ModelsApiControllerGcp(MojoScorer scorer, SampleRequestBuilder sampleRequestBuilder) {
    super(scorer, sampleRequestBuilder);
  }
}
