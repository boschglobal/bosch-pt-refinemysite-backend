/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary.dto

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.company.api.CompanyId
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardStatusEnum
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.milestone.domain.MilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.project.ProjectAddressVo
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ProjectDto(
    // audit information not required at project level: we will use time of import and the
    // superintendent who executes the import
    val identifier: ProjectId,
    val client: String? = null,
    val description: String? = null,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val title: String,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddressVo? = null,
    val participants: Set<ParticipantDto>,
    val projectCrafts: List<ProjectCraftDto>,
    val workAreas: List<WorkAreaDto>,
    val milestones: List<MilestoneDto>,
    val tasks: List<TaskDto>,
    val relations: List<RelationDto>
)

sealed class ParticipantDto(
    open val identifier: ParticipantId,
    open val companyId: CompanyId?,
    open val userId: UserId?,
    open val role: ParticipantRoleEnum?,
    open val status: ParticipantStatusEnum?
)

data class ActiveParticipantDto(
    override val identifier: ParticipantId,
    override val companyId: CompanyId,
    override val userId: UserId,
    override val role: ParticipantRoleEnum
) : ParticipantDto(identifier, companyId, userId, role, ACTIVE)

data class OtherParticipantDto(
    override val identifier: ParticipantId,
    override val companyId: CompanyId?,
    override val userId: UserId?,
    override val role: ParticipantRoleEnum? = null,
    override val status: ParticipantStatusEnum? = null
) : ParticipantDto(identifier, companyId, userId, role, status)

data class ProjectCraftDto(val identifier: ProjectCraftId, val name: String, val color: String)

data class WorkAreaDto(val identifier: WorkAreaId, val name: String)

data class MilestoneDto(
    val identifier: MilestoneId,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val projectCraft: ProjectCraftId? = null,
    val workArea: WorkAreaId? = null,
    val description: String? = null,
)

data class TaskDto(
    val identifier: TaskId,
    val name: String,
    val description: String?,
    val location: String?,
    val projectCraft: ProjectCraftId,
    val assignee: ParticipantId?,
    val workArea: WorkAreaId?,
    val status: TaskStatusEnum,
    // At the moment, we do not export or import edit dates.
    // val editDate: Date?,
    val start: LocalDate?,
    val end: LocalDate?,
    val dayCards: List<DayCardDto> = emptyList(),
    val topics: List<TopicDto> = emptyList(),
)

data class DayCardDto(
    val identifier: DayCardId,
    val date: LocalDate,
    val title: String,
    val manpower: BigDecimal,
    val notes: String? = null,
    val status: DayCardStatusEnum?,
    val reason: DayCardReasonEnum? = null
)

data class RelationDto(
    val type: RelationTypeEnum,
    val source: RelationElementDto,
    val target: RelationElementDto,
    val criticality: Boolean?
)

data class RelationElementDto(val id: UUID, val type: RelationElementTypeEnum)

data class TopicDto(
    val identifier: TopicId,
    val criticality: TopicCriticalityEnum,
    val description: String?,
    val messages: List<MessageDto>
)

data class MessageDto(
    val identifier: MessageId,
    val timestamp: LocalDateTime,
    val author: UserId,
    val content: String?
)
