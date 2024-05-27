/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.taskstatistics.boundary

import com.bosch.pt.iot.smartsite.application.SmartSiteMockKTest
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatisticsEntry
import com.bosch.pt.iot.smartsite.project.taskstatistics.repository.TaskStatisticsRepository
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.UNCRITICAL
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Unit tests for the implementation of the TaskStatisticsBoundaryService. */
@SmartSiteMockKTest
class TaskStatisticsServiceTest {

  @MockK(relaxed = true) private lateinit var taskStatisticsRepository: TaskStatisticsRepository

  @InjectMockKs private lateinit var cut: TaskStatisticsService

  /** Verifies that task statistics are return for a task identifier. */
  @Test
  fun verifyGetTaskStatistics() {
    val taskId = TaskId()
    val entries: List<TaskStatisticsEntry> =
        listOf(TaskStatisticsEntry(2, UNCRITICAL, taskId), TaskStatisticsEntry(1, CRITICAL, taskId))

    every { taskStatisticsRepository.getTaskStatistics(any()) } returns entries

    val returnedTaskStatistics = cut.findTaskStatistics(taskId)
    verify { taskStatisticsRepository.getTaskStatistics(any()) }

    assertThat(returnedTaskStatistics).isNotNull
    assertThat(returnedTaskStatistics.criticalTopics).isEqualTo(1)
    assertThat(returnedTaskStatistics.uncriticalTopics).isEqualTo(2)
  }
}
