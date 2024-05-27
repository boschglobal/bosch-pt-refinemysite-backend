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

data class SearchTasksDto(
    val projectIdentifier: ProjectId,
    val taskStatus: List<TaskStatusEnum> = emptyList(),
    val projectCraftIdentifiers: List<ProjectCraftId> = emptyList(),
    val workAreaIdentifiers: List<WorkAreaIdOrEmpty> = emptyList(),
    val assignees: AssigneesFilterDto = AssigneesFilterDto(emptyList(), emptyList()),
    val rangeStartDate: LocalDate? = null,
    val rangeEndDate: LocalDate? = null,
    val topicCriticality: List<TopicCriticalityEnum> = emptyList(),
    val hasTopics: Boolean? = null,
    val allDaysInDateRange: Boolean? = null
) {

  fun toTaskFilterDto() =
      TaskFilterDto(
          projectRef = projectIdentifier,
          taskStatus = taskStatus.ifEmpty { listOf(*TaskStatusEnum.values()) },
          projectCraftIds = projectCraftIdentifiers,
          workAreaIds = workAreaIdentifiers,
          assigneeIds = assignees.participantIdentifiers,
          assignedCompanies = assignees.companyIdentifiers,
          rangeStartDate = rangeStartDate,
          rangeEndDate = rangeEndDate,
          topicCriticality = if (hasTopics == null) topicCriticality else emptyList(),
          hasTopics = hasTopics,
          allDaysInDateRange = allDaysInDateRange)
}

data class AssigneesFilterDto(
    val participantIdentifiers: List<ParticipantId> = emptyList(),
    val companyIdentifiers: List<UUID> = emptyList()
)
