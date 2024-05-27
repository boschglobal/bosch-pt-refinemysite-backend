/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.copy.boundary

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.iot.smartsite.company.api.asCompanyId
import com.bosch.pt.iot.smartsite.project.copy.boundary.ExportSettings.Companion.exportEverything
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ActiveParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.MilestoneDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.OtherParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ParticipantDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectCraftDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.ProjectDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TaskDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.TopicDto
import com.bosch.pt.iot.smartsite.project.copy.boundary.dto.WorkAreaDto
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantStatusEnum.ACTIVE
import com.bosch.pt.iot.smartsite.project.participant.shared.repository.ParticipantRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.command.snapshotstore.toValueObject
import com.bosch.pt.iot.smartsite.project.project.query.ProjectQueryService
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository.ProjectCraftRepository
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GenericExporter(
    private val projectQueryService: ProjectQueryService,
    private val participantRepository: ParticipantRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    private val workAreaListRepository: WorkAreaListRepository,
    private val milestoneListRepository: MilestoneListRepository,
    private val taskRepository: TaskRepository,
    private val topicRepository: TopicRepository,
    private val relationRepository: RelationRepository,
    private val dayCardRepository: DayCardRepository
) {
  @Trace(operationName = "export project")
  @Transactional(readOnly = true)
  fun export(projectId: ProjectId, exportSettings: ExportSettings = exportEverything): ProjectDto {
    val project = requireNotNull(projectQueryService.findOneByIdentifier(projectId))

    val milestones =
        if (exportSettings.exportMilestones) exportMilestones(projectId) else emptyList()
    val tasks =
        if (exportSettings.exportTasks) exportTasks(projectId, exportSettings) else emptyList()

    return ProjectDto(
        identifier = project.identifier,
        client = project.client,
        description = project.description,
        start = project.start,
        end = project.end,
        projectNumber = project.projectNumber,
        title = project.title,
        category = project.category,
        address = project.projectAddress!!.toValueObject(),
        participants =
            if (exportSettings.exportParticipants) exportParticipants(projectId) else emptySet(),
        projectCrafts =
            if (exportSettings.exportCrafts) exportProjectCrafts(projectId) else emptyList(),
        workAreas = if (exportSettings.exportWorkAreas) exportWorkAreas(projectId) else emptyList(),
        milestones = milestones,
        tasks = tasks,
        relations =
            if (exportSettings.exportRelations)
                exportRelations(
                    projectId,
                    milestones.map { it.identifier.toUuid() } +
                        tasks.map { it.identifier.toUuid() })
            else emptyList())
  }

  private fun exportParticipants(projectId: ProjectId): Set<ParticipantDto> {
    return participantRepository
        .findAllByProjectIdentifier(projectId)
        .map {
          if (it.status == ACTIVE)
              ActiveParticipantDto(
                  identifier = it.identifier,
                  companyId = requireNotNull(it.company?.identifier).asCompanyId(),
                  userId = requireNotNull(it.user?.identifier).asUserId(),
                  role = requireNotNull(it.role))
          else
              OtherParticipantDto(
                  identifier = it.identifier,
                  companyId = it.company?.identifier?.asCompanyId(),
                  userId = it.user?.identifier?.asUserId(),
                  role = it.role,
                  status = it.status)
        }
        .toSet()
  }

  private fun exportProjectCrafts(projectId: ProjectId): List<ProjectCraftDto> =
      projectCraftRepository.findAllByProjectIdentifier(projectId).map {
        ProjectCraftDto(it.identifier, it.name, it.color)
      }

  private fun exportWorkAreas(projectId: ProjectId): List<WorkAreaDto> =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectId)?.workAreas?.map {
        WorkAreaDto(requireNotNull(it.identifier), requireNotNull(it.name))
      } ?: emptyList()

  private fun exportMilestones(projectId: ProjectId): List<MilestoneDto> =
      milestoneListRepository.findAllWithDetailsByProjectIdentifier(projectId).flatMap {
        it.milestones.map { milestone ->
          MilestoneDto(
              identifier = milestone.identifier,
              name = milestone.name,
              type = milestone.type,
              date = milestone.date,
              header = milestone.header,
              projectCraft = milestone.craft?.identifier,
              workArea = milestone.workArea?.identifier,
              description = milestone.description)
        }
      }

  private fun exportTasks(projectId: ProjectId, exportSettings: ExportSettings): List<TaskDto> {
    val topicsByTaskId =
        if (exportSettings.exportTopics)
            topicRepository
                .findAllByTaskProjectIdentifierAndDeletedFalse(projectId)
                .filter { it.task != null }
                .groupBy { it.task.identifier }
        else emptyMap()
    return taskRepository
        .findAllWithDetailsByProjectIdentifier(projectId)
        .also {
          if (exportSettings.exportDayCards) {
            // populate Hibernate persistence context with DayCards needed in the next step
            // to avoid n+1 queries
            dayCardRepository.findAllByTaskScheduleProjectIdentifier(projectId)
          }
        }
        .map { task ->
          TaskDto(
              identifier = requireNotNull(task.identifier),
              name = requireNotNull(task.name),
              description = task.description,
              location = task.location,
              projectCraft = task.projectCraft.identifier,
              assignee = task.assignee?.identifier,
              workArea = task.workArea?.identifier,
              status =
                  if (exportSettings.exportTaskStatus) requireNotNull(task.status)
                  else TaskStatusEnum.DRAFT,
              start = task.taskSchedule?.start,
              end = task.taskSchedule?.end,
              dayCards =
                  if (exportSettings.exportDayCards)
                      task.taskSchedule?.slots?.map { slot ->
                        DayCardDto(
                            identifier = requireNotNull(slot.dayCard.identifier),
                            date = slot.date,
                            title = slot.dayCard.title,
                            manpower = slot.dayCard.manpower,
                            notes = slot.dayCard.notes,
                            status = slot.dayCard.status,
                            reason = slot.dayCard.reason)
                      } ?: emptyList()
                  else emptyList(),
              topics =
                  (topicsByTaskId[task.identifier] ?: emptyList()).map { topic ->
                    TopicDto(
                        identifier = requireNotNull(topic.identifier),
                        criticality = requireNotNull(topic.criticality),
                        description = topic.description,
                        messages =
                            topic
                                .getMessages()
                                .map { message ->
                                  MessageDto(
                                      identifier = message.identifier,
                                      timestamp = message.createdDate.get(),
                                      author = message.createdBy.get().identifier.asUserId(),
                                      content = message.content)
                                }
                                .sortedBy { it.timestamp })
                  })
        }
  }

  private fun exportRelations(
      projectId: ProjectId,
      milestonesAndTasks: List<UUID>,
  ): List<RelationDto> =
      relationRepository
          .findAllByProjectIdentifier(projectId)
          .filter {
            milestonesAndTasks.contains(it.source.identifier) &&
                milestonesAndTasks.contains(it.target.identifier)
          }
          .map {
            RelationDto(
                type = it.type,
                source = RelationElementDto(it.source.identifier, it.source.type),
                target = RelationElementDto(it.target.identifier, it.target.type),
                criticality = it.critical)
          }
}

data class ExportSettings(
    val exportParticipants: Boolean,
    val exportCrafts: Boolean,
    val exportWorkAreas: Boolean,
    val exportTasks: Boolean,
    val exportTaskStatus: Boolean,
    val exportMilestones: Boolean,
    val exportDayCards: Boolean,
    val exportRelations: Boolean,
    val exportTopics: Boolean
) {

  companion object {
    val exportEverything =
        ExportSettings(
            exportParticipants = true,
            exportCrafts = true,
            exportWorkAreas = true,
            exportTasks = true,
            exportTaskStatus = true,
            exportMilestones = true,
            exportDayCards = true,
            exportRelations = true,
            exportTopics = true)
    val exportOnlyBasicProjectData =
        ExportSettings(
            exportParticipants = false,
            exportCrafts = false,
            exportWorkAreas = false,
            exportTasks = false,
            exportTaskStatus = false,
            exportMilestones = false,
            exportDayCards = false,
            exportRelations = false,
            exportTopics = false)
  }
}
