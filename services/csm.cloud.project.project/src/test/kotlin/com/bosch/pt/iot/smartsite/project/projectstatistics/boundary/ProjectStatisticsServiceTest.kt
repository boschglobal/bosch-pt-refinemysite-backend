/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectstatistics.boundary

import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.TaskStatusStatisticsEntry
import com.bosch.pt.iot.smartsite.project.projectstatistics.model.TopicCriticalityStatisticsEntry
import com.bosch.pt.iot.smartsite.project.projectstatistics.repository.ProjectStatisticsRepository
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.ACCEPTED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.CLOSED
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.DRAFT
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.STARTED
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import com.bosch.pt.iot.smartsite.user.model.UserBuilder.Companion.user
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@SmartSiteMockKTest
class ProjectStatisticsServiceTest {

  @RelaxedMockK private lateinit var projectStatisticsRepository: ProjectStatisticsRepository

  @InjectMockKs private lateinit var cut: ProjectStatisticsService

  @Test
  fun `verify get task statistics`() {
    authenticate()

    val entityIdentifier = ProjectId()
    every { projectStatisticsRepository.getTaskStatistics(any()) } returns
        listOf(
            TaskStatusStatisticsEntry(1L, DRAFT, entityIdentifier),
            TaskStatusStatisticsEntry(2L, OPEN, entityIdentifier),
            TaskStatusStatisticsEntry(3L, STARTED, entityIdentifier),
            TaskStatusStatisticsEntry(4L, CLOSED, entityIdentifier),
            TaskStatusStatisticsEntry(5L, ACCEPTED, entityIdentifier))

    val result = cut.getTaskStatistics(entityIdentifier)

    verify { projectStatisticsRepository.getTaskStatistics(any()) }

    assertThat(result)
        .containsEntry(DRAFT, 1L)
        .containsEntry(OPEN, 2L)
        .containsEntry(STARTED, 3L)
        .containsEntry(CLOSED, 4L)
        .containsEntry(ACCEPTED, 5L)
  }

  @Test
  fun `verify get topic statistics`() {
    authenticate()

    val entityIdentifier = ProjectId()
    every { projectStatisticsRepository.getTopicStatistics(any()) } returns
        listOf(
            TopicCriticalityStatisticsEntry(1L, CRITICAL, entityIdentifier),
            TopicCriticalityStatisticsEntry(2L, UNCRITICAL, entityIdentifier))

    val result = cut.getTopicStatistics(entityIdentifier)

    verify { projectStatisticsRepository.getTopicStatistics(any()) }
    assertThat(result).containsEntry(CRITICAL, 1L).containsEntry(UNCRITICAL, 2L)
  }

  private fun authenticate() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
            user().build(), null, listOf(SimpleGrantedAuthority("ROLE_CSM")))
  }
}
