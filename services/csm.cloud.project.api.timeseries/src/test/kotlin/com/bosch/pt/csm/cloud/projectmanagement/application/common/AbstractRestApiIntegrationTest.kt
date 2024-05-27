/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.projectmanagement.application.config.ApiVersionProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
abstract class AbstractRestApiIntegrationTest : AbstractIntegrationTest() {

  @Autowired lateinit var mockMvc: MockMvc

  @Autowired lateinit var objectMapper: ObjectMapper

  @Autowired lateinit var apiVersionProperties: ApiVersionProperties

  fun latestAuthenticationStatusApi(path: String) =
      "/v${apiVersionProperties.authenticationStatus.max}/$path"

  fun latestCompanyApi(path: String) = "/v${apiVersionProperties.company.max}/$path"

  fun latestProjectApi(path: String) = "/v${apiVersionProperties.project.max}/$path"

  fun latestUserApi(path: String) = "/v${apiVersionProperties.user.max}/$path"

  fun latestTranslationApi(path: String) = "/v${apiVersionProperties.translation.max}/$path"

  fun <T> query(path: String, latestOnly: Boolean, resultType: Class<T>) =
      mockMvc.get("$path?latestOnly=$latestOnly").andExpect { status { isOk() } }.entity(resultType)

  fun <T> ResultActionsDsl.entityList(entityType: Class<T>): List<T> =
      objectMapper.let {
        val type = it.typeFactory.constructType(entityType.componentType)
        it.convertValue(this.andReturn().response.contentAsByteArray, type)
      }

  fun <T> ResultActionsDsl.entity(entityType: Class<T>): T =
      objectMapper.readValue(this.andReturn().response.contentAsByteArray, entityType)
}
