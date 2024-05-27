/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.common

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ObjectReferenceDto
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import java.util.Locale
import org.apache.commons.text.StringSubstitutor
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.test.web.servlet.setup.MockMvcBuilders

abstract class AbstractActivityIntegrationTest : AbstractIntegrationTest() {

  @Autowired lateinit var messageSource: MessageSource

  @BeforeEach
  fun setupIntegration() {
    Locale.setDefault(Locale.ENGLISH)

    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
  }

  fun findLatestActivity(): Activity = repositories.activityRepository.findAll().last()

  fun buildSummary(
      messageKey: String,
      indexedArguments: Array<String> = arrayOf(),
      namedArguments: Map<String, String> = mapOf(),
      objectReferences: Map<String, ObjectReferenceDto> = mapOf()
  ): SummaryDto {
    val message = translate(messageKey, *indexedArguments)
    val template = substituteNamedArguments(message, namedArguments)
    return SummaryDto(template = template, references = objectReferences)
  }

  fun buildPlaceholder(aggregateIdentifier: AggregateIdentifierAvro, text: String) =
      ObjectReferenceDto(
          type = AggregateType.valueOf(aggregateIdentifier.getType()).type,
          id = aggregateIdentifier.getIdentifier().toUUID(),
          text = text)

  fun displayName(user: UserAggregateAvro) = "${user.getFirstName()} ${user.getLastName()}"

  fun translate(messageKey: String, vararg args: String = arrayOf()): String =
      messageSource.getMessage(messageKey, arrayOf(*args), LocaleContextHolder.getLocale())

  private fun substituteNamedArguments(template: String, namedArguments: Map<String, String>) =
      StringSubstitutor(namedArguments, "\${", "}").replace(template)
}
