/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getProjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro.CUSTOM1
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.rfv.model.RfvCustomization
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RestoreRfvCustomizationStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()
  }

  @Test
  fun `validate that rfv created event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator.submitRfvCustomization("rfv1") {
        it.key = CUSTOM1
        it.name = "Custom rfv"
        it.active = false
      }
    }

    val aggregate = get<RfvCustomizationAggregateAvro>("rfv1")!!

    transactionTemplate.executeWithoutResult {
      val rfv = repositories.findRfvWithDetails(getIdentifier("rfv1"))!!

      assertThat(rfv.version).isEqualTo(0L)
      validateBasicAttributes(rfv, aggregate)
      validateAuditableAndVersionedEntityAttributes(rfv, aggregate)
    }
  }

  @Test
  fun `validate that rfv updated event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitRfvCustomization("rfv1") {
            it.key = CUSTOM1
            it.name = "Custom rfv"
            it.active = false
          }
          .submitRfvCustomization(asReference = "rfv1", eventType = UPDATED) { it.active = true }
    }

    val aggregate = get<RfvCustomizationAggregateAvro>("rfv1")!!

    transactionTemplate.executeWithoutResult {
      val rfv = repositories.findRfvWithDetails(getIdentifier("rfv1"))!!

      assertThat(rfv.version).isEqualTo(1L)
      validateBasicAttributes(rfv, aggregate)
      validateAuditableAndVersionedEntityAttributes(rfv, aggregate)
    }
  }

  @Test
  fun `validate that rfv delete event was processed successfully`() {
    eventStreamGenerator.repeat {
      eventStreamGenerator
          .submitRfvCustomization("rfv1") {
            it.key = CUSTOM1
            it.name = "Custom rfv"
            it.active = false
          }
          .submitRfvCustomization(asReference = "rfv1", eventType = DELETED)
    }

    transactionTemplate.executeWithoutResult {
      repositories.findRfvWithDetails(getIdentifier("rfv1")).also { assertThat(it).isNull() }
    }
  }

  private fun validateBasicAttributes(
      rfvCustomization: RfvCustomization,
      aggregate: RfvCustomizationAggregateAvro
  ) {
    assertThat(rfvCustomization.identifier)
        .isEqualTo(aggregate.getAggregateIdentifier().getIdentifier().toUUID())
    assertThat(rfvCustomization.version).isEqualTo(aggregate.getAggregateIdentifier().getVersion())
    assertThat(rfvCustomization.project.identifier)
        .isEqualTo(aggregate.getProjectIdentifier().asProjectId())
    assertThat(rfvCustomization.name).isEqualTo(aggregate.getName())
    assertThat(rfvCustomization.key.name).isEqualTo(aggregate.getKey().name)
    assertThat(rfvCustomization.active).isEqualTo(aggregate.getActive())
  }
}
