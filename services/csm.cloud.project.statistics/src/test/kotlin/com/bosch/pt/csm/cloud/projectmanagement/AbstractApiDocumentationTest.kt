/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureMockMvc
@AutoConfigureRestDocs
abstract class AbstractApiDocumentationTest : AbstractApiIntegrationTest() {

  @Autowired protected lateinit var mockMvc: MockMvc

  protected fun latestVersionOf(path: String): String = "/v${apiVersionProperties.version.max}$path"
}
