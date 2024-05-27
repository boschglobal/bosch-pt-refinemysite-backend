/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersionProperties
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import org.springframework.beans.factory.annotation.Autowired

@EnableKafkaListeners
@SmartSiteSpringBootTest
abstract class AbstractApiIntegrationTest : AbstractIntegrationTest() {

  @Autowired protected lateinit var apiVersionProperties: ApiVersionProperties
}
