/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.application.config.EnableAllKafkaListeners
import com.bosch.pt.csm.cloud.application.RmsSpringBootTest
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
@RmsSpringBootTest
abstract class AbstractApiIntegrationTest : AbstractIntegrationTest() {

  @Autowired protected lateinit var apiVersionProperties: ApiVersionProperties
}
