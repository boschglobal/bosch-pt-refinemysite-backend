/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkdayConfiguration
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.DayEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.HolidayAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workday.messages.WorkdayConfigurationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_HOLIDAY
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_WORKDAY
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.facade.rest.ProjectController
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.request.SaveProjectResource
import com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response.ProjectAddressDto
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.HolidayResource
import com.bosch.pt.iot.smartsite.project.workday.facade.rest.resource.request.UpdateWorkdayConfigurationResource
import com.bosch.pt.iot.smartsite.project.workday.util.WorkdayConfigurationTestUtils.verifyCreatedAggregate
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate.now
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class WorkdayConfigurationIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: WorkdayConfigurationController

  @Autowired private lateinit var projectController: ProjectController

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Nested
  inner class `Create workday configuration` {

    @Test
    fun `verify create workday configuration succeeds when creating a project`() {
      val projectIdentifier = ProjectId()
      val newProjectResource =
          SaveProjectResource(
              client = "client",
              description = "description",
              start = now(),
              end = now().plus(1, ChronoUnit.DAYS),
              projectNumber = "projectNumber",
              title = "newCreatedProject",
              category = ProjectCategoryEnum.OB,
              address = ProjectAddressDto("city", "HN", "street", "ZC"))

      projectController.createProject(projectIdentifier, newProjectResource)
      val workdayConfigurationResource = cut.find(projectIdentifier).body!!

      assertThat(workdayConfigurationResource).isNotNull
      assertThat(workdayConfigurationResource.startOfWeek).isEqualTo(MONDAY)
      assertThat(workdayConfigurationResource.workingDays)
          .containsExactly(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
      assertThat(workdayConfigurationResource.holidays).isEmpty()
      assertThat(workdayConfigurationResource.allowWorkOnNonWorkingDays).isTrue

      projectEventStoreUtils
          .verifyContainsAndGet(
              WorkdayConfigurationEventAvro::class.java,
              WorkdayConfigurationEventEnumAvro.CREATED,
              1,
              false)
          .also { verifyCreatedAggregate(it[0].aggregate, projectIdentifier, testUser) }
    }
  }

  @Nested
  inner class `Find workday configuration` {

    @Test
    fun `verify find workday configuration fails when workday configuration not found`() {
      eventStreamGenerator
          .submitProject(asReference = "projectWithoutWorkdayConfiguration")
          .submitParticipantG3(asReference = "anotherParticipantCsm2") {
            it.user = getByReference("userCsm2")
            it.role = CSM
          }

      assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
        cut.find(getIdentifier("projectWithoutWorkdayConfiguration").asProjectId())
      }

      projectEventStoreUtils.verifyEmpty()
    }
  }

  @Nested
  inner class `Update workday configuration` {

    @Test
    fun `verify update workday configuration succeeds with same workdays in a different given order`() {
      eventStreamGenerator.submitWorkdayConfiguration(
          asReference = "workdayConfiguration", eventType = UPDATED) {
            it.startOfWeek = DayEnumAvro.MONDAY
            it.workingDays =
                listOf(
                    DayEnumAvro.MONDAY,
                    DayEnumAvro.TUESDAY,
                    DayEnumAvro.WEDNESDAY,
                    DayEnumAvro.THURSDAY,
                    DayEnumAvro.FRIDAY)
            it.holidays =
                listOf(
                    HolidayAvro("holiday", ACTUAL_DATE.toEpochMilli()),
                    HolidayAvro("holiday", ACTUAL_DATE.plusDays(1).toEpochMilli()))
            it.allowWorkOnNonWorkingDays = true
          }

      val updateWorkdayConfigurationResource =
          UpdateWorkdayConfigurationResource(
              MONDAY,
              listOf(WEDNESDAY, FRIDAY, MONDAY, THURSDAY, TUESDAY),
              listOf(
                  HolidayResource("holiday", ACTUAL_DATE),
                  HolidayResource("holiday", ACTUAL_DATE.plusDays(1))),
              true)

      cut.updateWorkdayConfiguration(
          getIdentifier("project").asProjectId(),
          updateWorkdayConfigurationResource,
          ETag.from("1"))

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify update workday configuration succeeds with same holidays in a different given order`() {
      eventStreamGenerator.submitWorkdayConfiguration(
          asReference = "workdayConfiguration", eventType = UPDATED) {
            it.startOfWeek = DayEnumAvro.MONDAY
            it.workingDays =
                listOf(
                    DayEnumAvro.MONDAY,
                    DayEnumAvro.TUESDAY,
                    DayEnumAvro.WEDNESDAY,
                    DayEnumAvro.THURSDAY,
                    DayEnumAvro.FRIDAY)
            it.holidays =
                listOf(
                    HolidayAvro("holiday", ACTUAL_DATE.toEpochMilli()),
                    HolidayAvro("holiday", ACTUAL_DATE.plusDays(1).toEpochMilli()))
            it.allowWorkOnNonWorkingDays = true
          }

      val updateWorkdayConfigurationResource =
          UpdateWorkdayConfigurationResource(
              MONDAY,
              listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
              listOf(
                  HolidayResource("holiday", ACTUAL_DATE.plusDays(1)),
                  HolidayResource("holiday", ACTUAL_DATE)),
              true)

      cut.updateWorkdayConfiguration(
          getIdentifier("project").asProjectId(),
          updateWorkdayConfigurationResource,
          ETag.from("1"))

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify update workday configuration fails when workday configuration not found`() {
      eventStreamGenerator
          .submitProject(asReference = "projectWithoutWorkdayConfiguration")
          .submitParticipantG3(asReference = "anotherParticipantCsm2") {
            it.user = getByReference("userCsm2")
            it.role = CSM
          }

      val projectIdentifier = getIdentifier("projectWithoutWorkdayConfiguration").asProjectId()
      val updateWorkdayConfigurationResource =
          UpdateWorkdayConfigurationResource(
              MONDAY,
              listOf(MONDAY, WEDNESDAY),
              listOf(HolidayResource("holiday", ACTUAL_DATE)),
              true)

      assertThatExceptionOfType(IllegalArgumentException::class.java)
          .isThrownBy {
            cut.updateWorkdayConfiguration(
                projectIdentifier, updateWorkdayConfigurationResource, ETag.from("1"))
          }
          .withMessage("Could not find Project $projectIdentifier")

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify update workday configuration fails with multiple entries of the same workday`() {
      val updateWorkdayConfigurationResource =
          UpdateWorkdayConfigurationResource(
              MONDAY,
              listOf(MONDAY, MONDAY, WEDNESDAY, WEDNESDAY),
              listOf(HolidayResource("holiday", ACTUAL_DATE)),
              true)

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.updateWorkdayConfiguration(
                    getIdentifier("project").asProjectId(),
                    updateWorkdayConfigurationResource,
                    ETag.from("0"))
                .body!!
          }
          .withMessage(WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_WORKDAY)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify update workday configuration fails with multiple entries of the same holiday`() {
      val updateWorkdayConfigurationResource =
          UpdateWorkdayConfigurationResource(
              MONDAY,
              listOf(MONDAY, WEDNESDAY),
              listOf(
                  HolidayResource("holiday", ACTUAL_DATE), HolidayResource("holiday", ACTUAL_DATE)),
              true)

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.updateWorkdayConfiguration(
                    getIdentifier("project").asProjectId(),
                    updateWorkdayConfigurationResource,
                    ETag.from("0"))
                .body!!
          }
          .withMessage(WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_HOLIDAY)

      projectEventStoreUtils.verifyEmpty()
    }

    @Test
    fun `verify update workday configuration fails when multiple entries of the same name in different case`() {
      val updateWorkdayConfigurationResource =
          UpdateWorkdayConfigurationResource(
              MONDAY,
              listOf(MONDAY, WEDNESDAY),
              listOf(
                  HolidayResource("holiday", ACTUAL_DATE), HolidayResource("HoLiDaY", ACTUAL_DATE)),
              true)

      assertThatExceptionOfType(PreconditionViolationException::class.java)
          .isThrownBy {
            cut.updateWorkdayConfiguration(
                    getIdentifier("project").asProjectId(),
                    updateWorkdayConfigurationResource,
                    ETag.from("0"))
                .body!!
          }
          .withMessage(WORKDAY_CONFIGURATION_VALIDATION_ERROR_DUPLICATED_HOLIDAY)

      projectEventStoreUtils.verifyEmpty()
    }
  }

  companion object {
    // Static date to be used in the tests and avoid problems with date comparison
    val ACTUAL_DATE = now()
  }
}
