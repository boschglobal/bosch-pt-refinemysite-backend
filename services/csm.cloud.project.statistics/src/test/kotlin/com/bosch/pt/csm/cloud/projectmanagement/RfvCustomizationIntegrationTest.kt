/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.config.EnableKafkaListeners
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.RfvCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.RfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableKafkaListeners
@SmartSiteSpringBootTest
class RfvCustomizationIntegrationTest : AbstractIntegrationTest() {

  @Autowired private lateinit var rfvCustomizationService: RfvCustomizationService

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().setupRfvIntegrationTestData()
    setAuthentication(getIdentifier("csm-user"))
  }

  @Test
  fun `verify rfv created events are processed successfully`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator.submitRfvCustomization {
      it.name = "Test rfv1"
      it.key = CUSTOM1
    }

    val rfvs = repositories.rfvCustomizationRepository.findAll()
    assertThat(rfvs).hasSize(1)

    validateAttributes(rfvs.first(), get("rfvCustomization")!!)
  }

  @Test
  fun `verify rfv updated events are processed successfully`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator
        .submitRfvCustomization {
          it.name = "Test rfv1"
          it.key = CUSTOM1
        }
        .submitRfvCustomization(eventType = UPDATED) { it.name = "Updated name" }

    val rfvs = repositories.rfvCustomizationRepository.findAll()
    assertThat(rfvs).hasSize(1)

    assertThat(rfvs.first().name).isEqualTo("Updated name")
    validateAttributes(rfvs.first(), get("rfvCustomization")!!)
  }

  @Test
  fun `verify rfv deleted events are processed successfully`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator
        .submitRfvCustomization {
          it.name = "Test rfv1"
          it.key = CUSTOM1
        }
        .submitRfvCustomization(eventType = DELETED)

    repositories.rfvCustomizationRepository.findAll().also { assertThat(it).isEmpty() }
  }

  @Test
  fun `verify rfvs are resolved correctly from database`() {
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(0)
    eventStreamGenerator.submitRfvCustomization {
      it.name = "Test rfv1"
      it.key = CUSTOM1
    }
    assertThat(repositories.rfvCustomizationRepository.findAll()).hasSize(1)

    val rfvs = rfvCustomizationService.resolveProjectRfvs(getIdentifier("project"))
    assertThat(rfvs).hasSize(16)

    assertThat(rfvs[DayCardReasonVarianceEnum.BAD_WEATHER]).isEqualTo("Weather")
    assertThat(rfvs[DayCardReasonVarianceEnum.MISSING_TOOLS]).isEqualTo("Missing / Defective tools")
    assertThat(rfvs[DayCardReasonVarianceEnum.MISSING_INFOS])
        .isEqualTo("Missing / Incomplete information")
    assertThat(rfvs[DayCardReasonVarianceEnum.TOUCHUP]).isEqualTo("Rework required")
    assertThat(rfvs[DayCardReasonVarianceEnum.OVERESTIMATION])
        .isEqualTo("Overestimation of own performance")
    assertThat(rfvs[DayCardReasonVarianceEnum.MANPOWER_SHORTAGE]).isEqualTo("Worker shortage")
    assertThat(rfvs[DayCardReasonVarianceEnum.CHANGED_PRIORITY]).isEqualTo("Client / Design change")
    assertThat(rfvs[DayCardReasonVarianceEnum.CONCESSION_NOT_RECOGNIZED])
        .isEqualTo("Preliminary work not recognized")
    assertThat(rfvs[DayCardReasonVarianceEnum.NO_CONCESSION]).isEqualTo("Preliminary work not done")
    assertThat(rfvs[DayCardReasonVarianceEnum.DELAYED_MATERIAL])
        .isEqualTo("Delayed / Defective materials")
    assertThat(rfvs[DayCardReasonVarianceEnum.CUSTOM1]).isEqualTo("Test rfv1")
    assertThat(rfvs[DayCardReasonVarianceEnum.CUSTOM2]).isEqualTo("Custom reason 2")
    assertThat(rfvs[DayCardReasonVarianceEnum.CUSTOM3]).isEqualTo("Custom reason 3")
    assertThat(rfvs[DayCardReasonVarianceEnum.CUSTOM4]).isEqualTo("Custom reason 4")
    assertThat(rfvs[DayCardReasonVarianceEnum.OTHER]).isEqualTo("Other")
  }

  private fun validateAttributes(
      rfvCustomization: RfvCustomization,
      aggregate: RfvCustomizationAggregateAvro
  ) {
    assertThat(rfvCustomization.name).isEqualTo(aggregate.name)
    assertThat(rfvCustomization.key.name).isEqualTo(aggregate.key.name)
    assertThat(rfvCustomization.active).isEqualTo(aggregate.active)
    assertThat(rfvCustomization.identifier).isEqualTo(aggregate.getIdentifier())
    assertThat(rfvCustomization.projectIdentifier).isEqualTo(aggregate.getProjectIdentifier())
  }
}
