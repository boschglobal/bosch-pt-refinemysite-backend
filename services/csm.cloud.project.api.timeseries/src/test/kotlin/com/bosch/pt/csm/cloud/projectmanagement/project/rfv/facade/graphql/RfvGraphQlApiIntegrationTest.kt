/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractGraphQlApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.model.DayCardReasonEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.daycard.extension.asReason
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response.RfvPayloadV1
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.test.eventDate
import com.bosch.pt.csm.cloud.projectmanagement.test.get
import com.bosch.pt.csm.cloud.projectmanagement.test.getList
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

@RmsSpringBootTest
class RfvGraphQlApiIntegrationTest : AbstractGraphQlApiIntegrationTest() {

  @Autowired private lateinit var messageSource: MessageSource

  private final val rfvs = "projects[0].rfvs"

  private val rfv = "$rfvs[0]"

  private val query =
      """
      query {
        projects {
          rfvs {
            id
            version
            reason
            name
            active
            eventDate
          }
        }
      }
      """
          .trimIndent()

  lateinit var aggregateV0: RfvCustomizationAggregateAvro

  lateinit var aggregateV1: RfvCustomizationAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
  }

  @Test
  fun `query rfv with customization`() {
    submitEvents()

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.getList(rfvs, RfvPayloadV1::class.java).hasSize(DayCardReasonEnum.values().size)

    response.get("$rfv.id").isEqualTo(aggregateV1.aggregateIdentifier.identifier)
    response.get("$rfv.version").isEqualTo(aggregateV1.aggregateIdentifier.version.toString())
    response.get("$rfv.reason").isEqualTo(aggregateV1.key.asReason().shortKey)
    response.get("$rfv.name").isEqualTo(aggregateV1.name)
    response.get("$rfv.active").isEqualTo(aggregateV1.active)
    response.get("$rfv.eventDate").isEqualTo(aggregateV1.eventDate())
  }

  @Test
  fun `query rfv without customization`() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    val reason = DayCardReasonEnum.CHANGED_PRIORITY

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.getList(rfvs, RfvPayloadV1::class.java).hasSize(DayCardReasonEnum.values().size)

    response.get("$rfv.id").isEqualTo(DayCardReasonEnum.CHANGED_PRIORITY.id.toString())
    response.get("$rfv.version").isEqualTo("-1")
    response.get("$rfv.reason").isEqualTo(reason.shortKey)
    response
        .get("$rfv.name")
        .isEqualTo(
            messageSource.getMessage(
                reason.messageKey, emptyArray(), LocaleContextHolder.getLocale()))
    response.get("$rfv.active").isEqualTo(!reason.isCustom)
    response.get("$rfv.eventDate").isEqualTo(reason.timestamp.toLocalDateTimeByMillis().toString())
  }

  @Test
  fun `query deleted rfv customization`() {
    val reason = DayCardReasonEnum.CHANGED_PRIORITY

    submitEvents()
    eventStreamGenerator.submitRfvCustomization(eventType = RfvCustomizationEventEnumAvro.DELETED)

    // Execute query and validate payload
    val response = graphQlTester.document(query).execute()
    response.getList(rfvs, RfvPayloadV1::class.java).hasSize(DayCardReasonEnum.values().size)

    response.get("$rfv.id").isEqualTo(DayCardReasonEnum.CHANGED_PRIORITY.id.toString())
    response.get("$rfv.version").isEqualTo("-1")
    response.get("$rfv.reason").isEqualTo(reason.shortKey)
    response
        .get("$rfv.name")
        .isEqualTo(
            messageSource.getMessage(
                reason.messageKey, emptyArray(), LocaleContextHolder.getLocale()))
    response.get("$rfv.active").isEqualTo(!reason.isCustom)
    response.get("$rfv.eventDate").isEqualTo(reason.timestamp.toLocalDateTimeByMillis().toString())
  }

  private fun submitEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()

    aggregateV0 =
        eventStreamGenerator
            .submitRfvCustomization {
              it.key = DayCardReasonNotDoneEnumAvro.CHANGED_PRIORITY
              it.name = "Important"
            }
            .get("rfvCustomization")!!

    aggregateV1 =
        eventStreamGenerator
            .submitRfvCustomization(eventType = RfvCustomizationEventEnumAvro.UPDATED) {
              it.name = "Very important"
            }
            .get("rfvCustomization")!!
  }
}
