/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.shared.repository.dto

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum.OPEN
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.TaskFilterDto.Companion.buildForCalendar
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum.CRITICAL
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate.now
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TaskFilterDtoTest {

  @Test
  fun `Has topic is set to false when 'has topics' is null`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            listOf(CRITICAL),
            false,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
    assertThat(taskFilterDto.startAndEndDateMustBeSet).isTrue
    assertThat(taskFilterDto.taskStatus).isEqualTo(listOf(*TaskStatusEnum.values()))
    assertThat(taskFilterDto.hasTopics).isFalse
    assertThat(taskFilterDto.topicCriticality).isEmpty()
  }

  @Test
  fun `Topic criticality is set to critical when 'has topics' is  null`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            listOf(CRITICAL),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
    assertThat(taskFilterDto.startAndEndDateMustBeSet).isTrue
    assertThat(taskFilterDto.taskStatus).isEqualTo(listOf(*TaskStatusEnum.values()))
    assertThat(taskFilterDto.hasTopics).isNull()
    assertThat(taskFilterDto.topicCriticality).isEqualTo(listOf(CRITICAL))
  }

  @Test
  fun `Only single task status is set when it is explicitly specified`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            listOf(OPEN),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
    assertThat(taskFilterDto.startAndEndDateMustBeSet).isTrue
    assertThat(taskFilterDto.taskStatus).isEqualTo(listOf(OPEN))
  }

  @Test
  fun `Calendar filters applied returns false when nothing is set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isFalse
    assertThat(taskFilterDto.startAndEndDateMustBeSet).isTrue
    assertThat(taskFilterDto.taskStatus).isEqualTo(listOf(*TaskStatusEnum.values()))
  }

  @Test
  fun `Calendar filters applied returns true when 'task status' is set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            listOf(OPEN),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }

  @Test
  fun `Calendar filters applied returns true when 'project crafts' are set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            listOf(ProjectCraftId()),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }

  @Test
  fun `Calendar filters applied returns true when 'work areas' are set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            listOf(WorkAreaIdOrEmpty(WorkAreaId())),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }

  @Test
  fun `Calendar filters applied returns true when 'assigned participants' are set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(ParticipantId()),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }

  @Test
  fun `Calendar filters applied returns true when 'assigned companies' are set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            listOf(randomUUID()),
            now(),
            now().plusDays(10),
            emptyList(),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }

  @Test
  fun `Calendar filters applied returns true when 'topic criticality' is set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            listOf(CRITICAL),
            null,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }

  @Test
  fun `Calendar filters applied returns true when 'has topics' is set`() {
    val taskFilterDto =
        buildForCalendar(
            ProjectId(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            now(),
            now().plusDays(10),
            emptyList(),
            true,
            null)

    assertThat(taskFilterDto.hasCalendarFiltersApplied()).isTrue
  }
}
