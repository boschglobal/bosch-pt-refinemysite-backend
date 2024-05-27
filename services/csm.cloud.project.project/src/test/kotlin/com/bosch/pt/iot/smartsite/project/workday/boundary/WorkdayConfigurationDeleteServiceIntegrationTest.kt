/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workday.boundary

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.common.test.kafka.TestAcknowledgement
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.PROJECT
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.application.security.AuthorizationTestUtils.simulateKafkaListener
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractDeleteIntegrationTest
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.boundary.ProjectDeleteService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EnableAllKafkaListeners
class WorkdayConfigurationDeleteServiceIntegrationTest : AbstractDeleteIntegrationTest() {

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
  fun `verify delete of workday configuration succeeds`() {
    sendDeleteCommandForProject()
    assertThat(repositories.findWorkdayConfiguration(project.identifier)).isNull()
  }

  @Test
  fun `verify delete of non-exiting workday configuration succeeds`() {
    // Delete the workday configuration of the project
    repositories.workdayConfigurationRepository.deleteById(project.id!!)

    sendDeleteCommandForProject()
    assertThat(repositories.findWorkdayConfiguration(project.identifier)).isNull()
  }

  private fun sendDeleteCommandForProject() {
    simulateKafkaListener { projectDeleteService.markAsDeleted(project.identifier) }
    sendDeleteCommand(
        project.identifier.toUuid(), project.version, PROJECT, testUser, TestAcknowledgement())
  }
}
