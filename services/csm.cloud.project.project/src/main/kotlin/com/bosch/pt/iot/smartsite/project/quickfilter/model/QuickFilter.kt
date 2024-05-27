/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.quickfilter.model

import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaIdOrEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Sharded

@Sharded(shardKey = ["projectIdentifier"])
@Document(collection = "QuickFilter")
@TypeAlias("QuickFilter")
data class QuickFilter(
    @Id var identifier: QuickFilterId,
    @field:Size(min = 1, max = MAX_NAME_LENGTH) var name: String,
    var projectIdentifier: ProjectId,
    var participantIdentifier: ParticipantId,
    var useMilestoneCriteria: Boolean = false,
    var useTaskCriteria: Boolean = false,
    var highlight: Boolean = false,
    var milestoneCriteria: MilestoneCriteria,
    var taskCriteria: TaskCriteria
) {

  @Version var version: Long? = null

  @CreatedBy lateinit var createdBy: UUID

  @CreatedDate lateinit var createdDate: LocalDateTime

  @LastModifiedBy lateinit var lastModifiedBy: UUID

  @LastModifiedDate lateinit var lastModifiedDate: LocalDateTime

  companion object {
    const val MAX_QUICK_FILTERS_PER_PARTICIPANT_IN_PROJECT = 100
    const val MAX_NAME_LENGTH = 100
  }
}

@TypeAlias("MilestoneCriteria")
data class MilestoneCriteria(
    var from: LocalDate? = null,
    var to: LocalDate? = null,
    var workAreas: WorkAreas = WorkAreas(),
    var milestoneTypes: MilestoneTypes = MilestoneTypes(),
)

@TypeAlias("TaskCriteria")
data class TaskCriteria(
    var from: LocalDate? = null,
    var to: LocalDate? = null,
    var workAreaIds: Set<WorkAreaIdOrEmpty> = emptySet(),
    var projectCraftIds: Set<ProjectCraftId> = emptySet(),
    var allDaysInDateRange: Boolean? = null,
    var status: Set<TaskStatusEnum> = emptySet(),
    var assignees: AssigneesCriteria = AssigneesCriteria(),
    var hasTopics: Boolean? = null,
    var topicCriticality: Set<TopicCriticalityEnum> = emptySet()
)

@TypeAlias("AssigneesCriteria")
data class AssigneesCriteria(
    var participantIds: Set<ParticipantId> = emptySet(),
    var companyIds: Set<UUID> = emptySet()
)

@TypeAlias("WorkAreas")
data class WorkAreas(
    var header: Boolean? = null,
    var workAreaIds: Set<WorkAreaIdOrEmpty> = emptySet()
)

@TypeAlias("MilestoneTypes")
data class MilestoneTypes(
    var types: Set<MilestoneTypeEnum> = emptySet(),
    var projectCraftIds: Set<ProjectCraftId> = emptySet()
)
