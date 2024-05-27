/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.AssigneesFilterDto
import com.bosch.pt.iot.smartsite.project.task.shared.repository.dto.SearchTasksDto
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls.AS_EMPTY
import java.time.LocalDate
import java.util.UUID

data class FilterTaskListResource(
    @JsonSetter(nulls = AS_EMPTY) val assignees: FilterAssigneeResource = FilterAssigneeResource(),
    @JsonSetter(nulls = AS_EMPTY) val projectCraftIds: List<ProjectCraftId> = emptyList(),
    @JsonSetter(nulls = AS_EMPTY) val workAreaIds: List<WorkAreaIdOrEmpty> = emptyList(),
    @JsonSetter(nulls = AS_EMPTY) val status: List<TaskStatusEnum> = emptyList(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    @JsonSetter(nulls = AS_EMPTY) val topicCriticality: List<TopicCriticalityEnum> = emptyList(),
    val hasTopics: Boolean? = null,
    val allDaysInDateRange: Boolean? = null
) {

  fun toSearchTasksDto(projectRef: ProjectId) =
      SearchTasksDto(
          projectIdentifier = projectRef,
          taskStatus = status,
          projectCraftIdentifiers = projectCraftIds,
          workAreaIdentifiers = workAreaIds,
          assignees = AssigneesFilterDto(assignees.participantIds, assignees.companyIds),
          rangeStartDate = from,
          rangeEndDate = to,
          topicCriticality = topicCriticality,
          hasTopics = hasTopics,
          allDaysInDateRange = allDaysInDateRange)

  class FilterAssigneeResource(
      @JsonSetter(nulls = AS_EMPTY) val participantIds: List<ParticipantId> = emptyList(),
      @JsonSetter(nulls = AS_EMPTY) val companyIds: List<UUID> = emptyList()
  )
}
