/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitRfvCustomization
import com.bosch.pt.csm.cloud.projectmanagement.rfv.message.getVersion
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardReasonNotDoneEnumAvro
import com.bosch.pt.csm.cloud.usermanagement.user.event.submitSystemUserAndActivate
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractApiDocumentationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.RFV_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.BAD_WEATHER
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.CHANGED_PRIORITY
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.CUSTOM1
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum.CUSTOM2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.request.UpdateRfvResource
import com.bosch.pt.iot.smartsite.project.rfv.facade.rest.resource.response.RfvResource
import com.bosch.pt.iot.smartsite.testdata.plainProjectWithCsm
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class RfvIntegrationTest : AbstractApiDocumentationTestV2() {

  @Autowired private lateinit var cut: RfvController

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.submitSystemUserAndActivate().plainProjectWithCsm()

    setAuthentication(getIdentifier("csm-user"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify that by default only the default RFVs are active`() {
    val rfvs = cut.findAll(projectIdentifier)

    val listResource = rfvs.body!!
    assertThat(listResource.items).hasSize(14)

    // 10 active standard RFVs
    assertThat(filterResources(listResource, false)).containsOnly(true).hasSize(10)

    // 4 inactive custom RFVs
    assertThat(filterResources(listResource, true)).containsOnly(false).hasSize(4)
  }

  @Test
  fun `verify that only standard plus active RFVs are returned`() {
    activateAndNameCustom1Rfv()

    val rfvs = cut.findAll(projectIdentifier)

    val listResource = rfvs.body!!
    assertThat(listResource.items).hasSize(14)

    // 10 active standard RFVs
    assertThat(filterResources(listResource, false)).containsOnly(true).hasSize(10)

    // 1 active custom RFV
    assertThat(filterResources(listResource, true).filter { it }).hasSize(1)

    // 3 inactive custom RFVs
    assertThat(filterResources(listResource, true).filter { !it }).hasSize(3)
  }

  @Test
  fun `verify that a standard rfv with a name is accepted (first PUT, CREATE event) and the name is ignored`() {
    cut.update(projectIdentifier, UpdateRfvResource(CHANGED_PRIORITY, false, "abcdef"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CHANGED_PRIORITY.name)
    assertThat(aggregate.name).isNull()
    assertThat(aggregate.active).isFalse
  }

  @Test
  fun `verify that a standard rfv with a name is accepted (second PUT, DELETE event) and the name is ignored`() {
    deactivateBadWeatherRfv()

    cut.update(projectIdentifier, UpdateRfvResource(BAD_WEATHER, true, "abcdef"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, DELETED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(BAD_WEATHER.name)
    assertThat(aggregate.name).isNull()
    assertThat(aggregate.active).isFalse
  }

  @Test
  fun `verify that a standard rfv without a name is accepted (first PUT, CREATE event)`() {
    cut.update(projectIdentifier, UpdateRfvResource(CHANGED_PRIORITY, false, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CHANGED_PRIORITY.name)
    assertThat(aggregate.name).isNull()
    assertThat(aggregate.active).isFalse
  }

  @Test
  fun `verify that a standard rfv without a name is accepted (second PUT, DELETE event)`() {
    deactivateBadWeatherRfv()

    cut.update(projectIdentifier, UpdateRfvResource(BAD_WEATHER, true, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, DELETED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(BAD_WEATHER.name)
    assertThat(aggregate.name).isNull()
    assertThat(aggregate.active).isFalse
  }

  @Test
  fun `verify that a standard rfv with default settings is ignored`() {
    cut.update(projectIdentifier, UpdateRfvResource(BAD_WEATHER, true, null))
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify that a standard rfv with default settings (and text) is ignored`() {
    cut.update(projectIdentifier, UpdateRfvResource(BAD_WEATHER, true, "abd"))
    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify that a custom rfv with a name is accepted (first PUT, CREATE event)`() {
    cut.update(projectIdentifier, UpdateRfvResource(CUSTOM2, true, "Custom Reason"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CUSTOM2.name)
    assertThat(aggregate.name).isEqualTo("Custom Reason")
    assertThat(aggregate.active).isTrue
  }

  @Test
  fun `verify that a custom rfv without a name is accepted (first PUT, CREATE event)`() {
    cut.update(projectIdentifier, UpdateRfvResource(CUSTOM2, true, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CUSTOM2.name)
    assertThat(aggregate.name).isNull()
    assertThat(aggregate.active).isTrue
  }

  @Test
  fun `verify that a custom rfv with a name is accepted (second PUT, UPDATE event)`() {
    activateAndNameCustom1Rfv()

    cut.update(projectIdentifier, UpdateRfvResource(CUSTOM1, true, "Changed Name"))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, UPDATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.name).isEqualTo("Changed Name")
    assertThat(aggregate.active).isTrue
  }

  @Test
  fun `verify that a custom rfv with a name is accepted (second PUT, DELETE event)`() {
    activateAndNameCustom1Rfv()

    cut.update(projectIdentifier, UpdateRfvResource(CUSTOM1, false, ""))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, DELETED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.getVersion()).isEqualTo(1L)
    assertThat(aggregate.key.name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.name).isEqualTo("This is a custom RFV")
    assertThat(aggregate.active).isTrue
  }

  @Test
  fun `verify that a custom rfv without a name is accepted (second PUT, UPDATE event)`() {
    activateAndNameCustom1Rfv()

    cut.update(projectIdentifier, UpdateRfvResource(CUSTOM1, true, null))
    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, UPDATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.name).isNull()
    assertThat(aggregate.active).isTrue
  }

  @Test
  fun `verify that at least one rfv has to be active`() {
    val allReasons = DayCardReasonEnum.getStandardRfvs().toMutableList()
    val singleReason = allReasons.removeLast()
    allReasons.forEach { reason ->
      eventStreamGenerator.submitRfvCustomization(asReference = reason.name) {
        it.active = false
        it.key = DayCardReasonNotDoneEnumAvro.valueOf(reason.name)
        it.name = null
      }
    }
    projectEventStoreUtils.reset()

    val throwable = catchThrowable {
      cut.update(projectIdentifier, UpdateRfvResource(singleReason, false, null))
    }

    assertThat(throwable).isInstanceOf(PreconditionViolationException::class.java)
    assertThat((throwable as PreconditionViolationException).messageKey)
        .isEqualTo(RFV_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `verify saving a disabled custom rfv is possible if only one other rfv is active`() {
    val allReasons = DayCardReasonEnum.getStandardRfvs().toMutableList()
    allReasons.remove(BAD_WEATHER)
    allReasons.forEach { reason ->
      eventStreamGenerator.submitRfvCustomization(asReference = reason.name) {
        it.active = false
        it.key = DayCardReasonNotDoneEnumAvro.valueOf(reason.name)
        it.name = null
      }
    }
    projectEventStoreUtils.reset()

    cut.update(projectIdentifier, UpdateRfvResource(CUSTOM1, false, "Updated name"))

    val event =
        projectEventStoreUtils.verifyContainsAndGet(
            RfvCustomizationEventAvro::class.java, CREATED, true)

    val aggregate = event.aggregate
    assertThat(aggregate.key.name).isEqualTo(CUSTOM1.name)
    assertThat(aggregate.name).isEqualTo("Updated name")
    assertThat(aggregate.active).isFalse
  }

  private fun activateAndNameCustom1Rfv() {
    eventStreamGenerator.submitRfvCustomization(asReference = "rfv-custom1") {
      it.active = true
      it.key = DayCardReasonNotDoneEnumAvro.CUSTOM1
      it.name = "This is a custom RFV"
    }

    projectEventStoreUtils.reset()
  }

  private fun deactivateBadWeatherRfv() {
    eventStreamGenerator.submitRfvCustomization(asReference = "rfv-custom1") {
      it.active = false
      it.key = DayCardReasonNotDoneEnumAvro.BAD_WEATHER
    }

    projectEventStoreUtils.reset()
  }
  private fun filterResources(rfvs: BatchResponseResource<RfvResource>, custom: Boolean) =
      rfvs.items.filter { it.key.isCustom == custom }.map { it.active }
}
