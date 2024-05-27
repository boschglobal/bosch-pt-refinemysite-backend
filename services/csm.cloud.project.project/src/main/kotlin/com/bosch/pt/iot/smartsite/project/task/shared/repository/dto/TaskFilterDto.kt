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
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import java.time.LocalDate
import java.util.UUID
import org.springframework.util.CollectionUtils.isEmpty

/**
 * Data transfer object to pass filter criteria for tasks. Passing null or empty list to a filter
 * criteria will skip the corresponding filter.
 */
class TaskFilterDto(
    val projectRef: ProjectId,
    val taskStatus: List<TaskStatusEnum>? = null,
    val projectCraftIds: List<ProjectCraftId> = emptyList(),
    val workAreaIds: List<WorkAreaIdOrEmpty> = emptyList(),
    val assigneeIds: List<ParticipantId>? = null,
    val assignedCompanies: List<UUID>? = null,
    val rangeStartDate: LocalDate? = null,
    val rangeEndDate: LocalDate? = null,
    val startAndEndDateMustBeSet: Boolean? = false,
    val topicCriticality: List<TopicCriticalityEnum> = emptyList(),
    val hasTopics: Boolean? = null,
    val allDaysInDateRange: Boolean? = null,
) {

  fun hasCalendarFiltersApplied(): Boolean =
      !((isEmpty(taskStatus) ||
          requireNotNull(taskStatus).containsAll(listOf(*TaskStatusEnum.values()))) &&
          isEmpty(projectCraftIds) &&
          isEmpty(workAreaIds) &&
          isEmpty(assigneeIds) &&
          isEmpty(assignedCompanies) &&
          isEmpty(topicCriticality) &&
          hasTopics == null &&
          allDaysInDateRange == null)

  companion object {

    @JvmStatic
    fun buildForCalendar(
        projectRef: ProjectId,
        taskStatus: List<TaskStatusEnum>,
        projectCraftIds: List<ProjectCraftId>,
        workAreaIds: List<WorkAreaIdOrEmpty>,
        assignedParticipants: List<ParticipantId>,
        assignedCompanies: List<UUID>,
        rangeStartDate: LocalDate,
        rangeEndDate: LocalDate,
        topicCriticality: List<TopicCriticalityEnum>,
        hasTopics: Boolean?,
        allDaysInDateRange: Boolean?,
    ): TaskFilterDto =
        TaskFilterDto(
            projectRef = projectRef,
            taskStatus = if (isEmpty(taskStatus)) listOf(*TaskStatusEnum.values()) else taskStatus,
            projectCraftIds = projectCraftIds,
            workAreaIds = workAreaIds,
            assigneeIds = assignedParticipants,
            assignedCompanies = assignedCompanies,
            rangeStartDate = rangeStartDate,
            rangeEndDate = rangeEndDate,
            startAndEndDateMustBeSet = true,
            topicCriticality = if (hasTopics == null) topicCriticality else emptyList(),
            hasTopics = hasTopics,
            allDaysInDateRange = allDaysInDateRange)
  }
}
