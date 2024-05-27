/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.facade.rest

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTopicG2
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro
import com.bosch.pt.iot.smartsite.application.config.EnableAllKafkaListeners
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.rest.AbstractIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.OK

@EnableAllKafkaListeners
class ProjectStatisticIntegrationTest : AbstractIntegrationTestV2() {

  @Autowired private lateinit var projectStatisticsController: ProjectStatisticsController

  private val project by lazy { repositories.findProject(getIdentifier("project").asProjectId())!! }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm1"))
    projectEventStoreUtils.reset()
  }

  @Test
  fun `verify get project statistics with open tasks and uncritical topics`() {
    eventStreamGenerator.submitTopicG2()

    val response = projectStatisticsController.getProjectStatistics(project.identifier)
    assertThat(response.statusCode).isEqualTo(OK)

    val projectStatisticsResource = response.body
    assertThat(projectStatisticsResource).isNotNull
    assertThat(projectStatisticsResource!!.openTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.uncriticalTopics).isEqualTo(1L)
    assertThat(projectStatisticsResource.draftTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.startedTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.closedTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.acceptedTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.criticalTopics).isEqualTo(0L)
  }

  @Test
  fun `verify get project statistics with open and started tasks and critical topics`() {
    eventStreamGenerator
        .submitTask("task-started") { it.status = TaskStatusEnumAvro.STARTED }
        .submitTopicG2("critical-topic") { it.criticality = TopicCriticalityEnumAvro.CRITICAL }

    val response = projectStatisticsController.getProjectStatistics(project.identifier)
    assertThat(response.statusCode).isEqualTo(OK)

    val projectStatisticsResource = response.body
    assertThat(projectStatisticsResource).isNotNull
    assertThat(projectStatisticsResource!!.openTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.uncriticalTopics).isEqualTo(1L)
    assertThat(projectStatisticsResource.draftTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.startedTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.closedTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.acceptedTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.criticalTopics).isEqualTo(1L)
  }

  @Test
  fun `verify get project statistics with draft, closed and accepted tasks`() {
    eventStreamGenerator
        .submitTask("task-closed") { it.status = TaskStatusEnumAvro.CLOSED }
        .submitTask("task-draft") { it.status = TaskStatusEnumAvro.DRAFT }
        .submitTask("task-accepted") { it.status = TaskStatusEnumAvro.ACCEPTED }

    val response = projectStatisticsController.getProjectStatistics(project.identifier)
    assertThat(response.statusCode).isEqualTo(OK)

    val projectStatisticsResource = response.body
    assertThat(projectStatisticsResource).isNotNull
    assertThat(projectStatisticsResource!!.openTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.uncriticalTopics).isEqualTo(1L)
    assertThat(projectStatisticsResource.draftTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.startedTasks).isEqualTo(0L)
    assertThat(projectStatisticsResource.closedTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.acceptedTasks).isEqualTo(1L)
    assertThat(projectStatisticsResource.criticalTopics).isEqualTo(0L)
  }
}
