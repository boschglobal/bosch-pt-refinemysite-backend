/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.boundary

import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro.CSM
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_INVALID_POSITION
import com.bosch.pt.iot.smartsite.common.i18n.Key.WORK_AREA_VALIDATION_ERROR_USED_NAME
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.command.handler.batch.CreateWorkAreaBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.util.withMessageKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class WorkAreaListServiceIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var cut: CreateWorkAreaBatchCommandHandler

  @Autowired private lateinit var projectRepository: ProjectRepository

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitProject("p2").submitParticipantG3("p2Csm1") {
      it.user = getByReference("userCsm1")
      it.role = CSM
    }
    setAuthentication("userCsm1")
    setAuthentication(getIdentifier("userCsm1"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `add multiple work areas in batch successfully`() {
    eventStreamGenerator.submitWorkArea("p2Wa1").submitWorkAreaList("p2wal")

    val projectIdentifier = getIdentifier("p2").asProjectId()
    val project = projectRepository.findOneByIdentifier(getIdentifier("p2").asProjectId())

    val workAreas =
        listOf(
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project!!.identifier,
                name = "workArea1",
                position = null,
                0),
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project.identifier,
                name = "workArea2",
                position = null,
                1))

    assertThat(cut.handle(projectIdentifier, workAreas)).hasSize(2)

    projectEventStoreUtils.verifyContains(
        WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 2, false)
    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 2, false)
  }

  @Test
  fun `add multiple work areas in batch successfully to existing list`() {
    eventStreamGenerator.submitWorkArea("p2Wa1").submitWorkAreaList("p2wal")

    val projectIdentifier = getIdentifier("p2").asProjectId()
    val project = projectRepository.findOneByIdentifier(projectIdentifier)

    val workAreas =
        listOf(
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project!!.identifier,
                name = "workArea1",
                position = null,
                0),
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project.identifier,
                name = "workArea2",
                position = null,
                1))

    assertThat(cut.handle(projectIdentifier, workAreas)).hasSize(2)

    projectEventStoreUtils.verifyContains(
        WorkAreaEventAvro::class.java, WorkAreaEventEnumAvro.CREATED, 2, false)
    projectEventStoreUtils.verifyContains(
        WorkAreaListEventAvro::class.java, WorkAreaListEventEnumAvro.ITEMADDED, 2, false)
  }

  @Test
  fun `create multiple work areas in batch fail if multiple projectIdentifier are in list of workAreas`() {
    val projectIdentifier = getIdentifier("p2").asProjectId()
    val project = projectRepository.findOneByIdentifier(projectIdentifier)
    val project1 = projectRepository.findOneByIdentifier(getIdentifier("project").asProjectId())

    val workAreas =
        listOf(
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project!!.identifier,
                name = "workArea1",
                position = null,
                0),
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project1!!.identifier,
                name = "workArea2",
                position = null,
                1))

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.handle(projectIdentifier, workAreas) }
        .withMessage("Multiple workAreas can only be created for one project at at time")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `create multiple work areas in batch fail if projectIdentifier doesn't match`() {
    val projectIdentifier = getIdentifier("p2").asProjectId()
    val project = projectRepository.findOneByIdentifier(getIdentifier("project").asProjectId())

    val workAreas =
        listOf(
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project!!.identifier,
                name = "workArea1",
                position = null,
                0),
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project.identifier,
                name = "workArea2",
                position = null,
                1))

    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy { cut.handle(projectIdentifier, workAreas) }
        .withMessage("WorkAreas cannot be created for a foreign project")

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `create multiple work areas in batch fail for work area name duplicates`() {
    eventStreamGenerator.submitWorkArea("p2Wa1").submitWorkAreaList("p2wal")

    val projectIdentifier = getIdentifier("p2").asProjectId()
    val project = projectRepository.findOneByIdentifier(getIdentifier("p2").asProjectId())

    val workAreas =
        listOf(
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project!!.identifier,
                name = "p2Wa1",
                position = null,
                0),
            CreateWorkAreaCommand(
                identifier = WorkAreaId(),
                projectRef = project.identifier,
                name = "p2Wa1",
                position = null,
                1))

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.handle(projectIdentifier, workAreas) }
        .withMessageKey(WORK_AREA_VALIDATION_ERROR_USED_NAME)

    projectEventStoreUtils.verifyEmpty()
  }

  @Test
  fun `create multiple work areas in batch fail if the number exceeds the allowed limit`() {
    val projectIdentifier = getIdentifier("p2").asProjectId()
    val project = projectRepository.findOneByIdentifier(projectIdentifier)

    val workAreas =
        (0..999).map {
          CreateWorkAreaCommand(
              identifier = WorkAreaId(),
              projectRef = project!!.identifier,
              name = "workArea$it",
              position = null,
              it.toLong())
        }

    assertThatExceptionOfType(PreconditionViolationException::class.java)
        .isThrownBy { cut.handle(projectIdentifier, workAreas) }
        .withMessageKey(WORK_AREA_VALIDATION_ERROR_INVALID_POSITION)

    projectEventStoreUtils.verifyEmpty()
  }
}
