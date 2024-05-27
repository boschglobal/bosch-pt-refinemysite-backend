/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.service

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantRoleEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitParticipantG3
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectEventEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractDeleteIntegrationTest
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.boundary.ProjectDeleteService
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftListId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class ProjectCraftDeleteServiceIntegrationTest : AbstractDeleteIntegrationTest() {

  @Autowired private lateinit var projectDeleteService: ProjectDeleteService

  private val testUser by lazy { repositories.findUser(getIdentifier("userCsm2"))!! }
  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm2"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify delete of project delete the project craft and craft list succeeds`() {
    val projectCraftIdentifier = getIdentifier("projectCraft").asProjectCraftId()
    val projectCraftListIdentifier = getIdentifier("projectCraftList").asProjectCraftListId()

    sendDeleteCommandForProject()

    // Check that craft and the craft list are deleted and there is no kafka event of them.
    assertThat(repositories.findProjectCraft(projectCraftIdentifier)).isNull()
    assertThat(repositories.findProjectCraftList(projectCraftListIdentifier)).isNull()
    projectEventStoreUtils.verifyContainsAndGet(
        ProjectEventAvro::class.java, ProjectEventEnumAvro.DELETED, 1, false)
  }

  @Test
  fun `verify delete of project delete with no project craft and craft list succeeds`() {
    // Create a new project with no craft or craft list
    eventStreamGenerator.submitProject(asReference = "anotherProject").submitParticipantG3(
        asReference = "anotherParticipantCsm2") {
          it.user = getByReference("userCsm2")
          it.role = ParticipantRoleEnumAvro.CSM
        }

    sendDeleteCommandForProject(getIdentifier("anotherProject").asProjectId())

    // Check that no kafka event of them.
    projectEventStoreUtils.verifyContainsAndGet(
        ProjectEventAvro::class.java, ProjectEventEnumAvro.DELETED, 1, false)
  }

  private fun sendDeleteCommandForProject(projectIdentifier: ProjectId = project.identifier) {
    simulateKafkaListener { projectDeleteService.markAsDeleted(project.identifier) }
    sendDeleteCommand(
        projectIdentifier.toUuid(), project.version, PROJECT, testUser, TestAcknowledgement())
  }
}
