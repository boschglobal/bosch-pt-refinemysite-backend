/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.query.service

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateByMillis
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.project.craft.query.repository.ProjectCraftRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.dayCard.query.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.event.listener.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.milestone.query.repository.MilestoneRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.query.repository.ParticipantRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.asProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.Project
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectAddress
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectCategoryEnum
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectMapper
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectVersion
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.repository.ProjectRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.repository.RelationRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.query.repository.RfvRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.task.query.repository.TaskRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.query.repository.TaskConstraintRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraintselection.query.repository.TaskConstraintSelectionRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.taskschedule.query.repository.TaskScheduleRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.repository.TopicRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.query.repository.WorkAreaRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.query.repository.WorkAreaListRepository
import com.bosch.pt.csm.cloud.projectmanagement.project.workday.query.repository.WorkDayConfigurationRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class ProjectProjector(
    private val repository: ProjectRepository,
    private val projectCraftRepository: ProjectCraftRepository,
    private val dayCardRepository: DayCardRepository,
    private val milestoneRepository: MilestoneRepository,
    private val participantRepository: ParticipantRepository,
    private val relationRepository: RelationRepository,
    private val rfvRepository: RfvRepository,
    private val taskConstraintRepository: TaskConstraintRepository,
    private val taskConstraintSelectionRepository: TaskConstraintSelectionRepository,
    private val taskRepository: TaskRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val topicRepository: TopicRepository,
    private val workAreaListRepository: WorkAreaListRepository,
    private val workAreaRepository: WorkAreaRepository,
    private val workDayConfigurationRepository: WorkDayConfigurationRepository
) {

  fun onProjectEvent(aggregate: ProjectAggregateAvro) {
    val existingProject = repository.findOneByIdentifier(aggregate.getIdentifier().asProjectId())

    if (existingProject == null ||
        aggregate.aggregateIdentifier.version > existingProject.version) {
      (existingProject?.updateFromProjectAggregate(aggregate) ?: aggregate.toNewProjection())
          .apply { repository.save(this) }
    }
  }

  fun onProjectDeletedEvent(key: AggregateEventMessageKey) {
    val projectId = key.aggregateIdentifier.identifier.asProjectId()
    if (repository.existsById(projectId)) {
      taskConstraintSelectionRepository.deleteAllByProject(projectId)
      dayCardRepository.deleteAllByProject(projectId)
      relationRepository.deleteAllByProject(projectId)
      milestoneRepository.deleteAllByProject(projectId)
      topicRepository.deleteAllByProject(projectId)
      taskScheduleRepository.deleteAllByProject(projectId)
      taskRepository.deleteAllByProject(projectId)
      taskConstraintRepository.deleteAllByProject(projectId)
      rfvRepository.deleteAllByProject(projectId)
      projectCraftRepository.deleteAllByProject(projectId)
      workAreaListRepository.deleteAllByProject(projectId)
      workAreaRepository.deleteAllByProject(projectId)
      workDayConfigurationRepository.deleteAllByProject(projectId)
      participantRepository.deleteAllByProject(projectId)
      repository.deleteById(projectId)
    }
  }

  private fun ProjectAggregateAvro.toNewProjection(): Project {
    val projectVersion = this.newProjectVersion()

    return ProjectMapper.INSTANCE.fromProjectVersion(
        projectVersion = projectVersion,
        identifier = ProjectId(aggregateIdentifier.identifier.toUUID()),
        history = listOf(projectVersion))
  }

  private fun Project.updateFromProjectAggregate(aggregate: ProjectAggregateAvro): Project {
    val projectVersion = aggregate.newProjectVersion()

    return ProjectMapper.INSTANCE.fromProjectVersion(
        projectVersion = projectVersion,
        identifier = this.identifier,
        history = this.history.toMutableList().also { it.add(projectVersion) })
  }

  private fun ProjectAggregateAvro.newProjectVersion(): ProjectVersion {
    val isNew = this.aggregateIdentifier.version == 0L
    val auditUser: UserId
    val auditDate: LocalDateTime
    if (isNew) {
      auditUser = UserId(this.auditingInformation.createdBy.identifier.toUUID())
      auditDate = this.auditingInformation.createdDate.toLocalDateTimeByMillis()
    } else {
      auditUser = UserId(this.auditingInformation.lastModifiedBy.identifier.toUUID())
      auditDate = this.auditingInformation.lastModifiedDate.toLocalDateTimeByMillis()
    }

    val category = this.category?.let { ProjectCategoryEnum.valueOf(it.name) }
    val address =
        this.projectAddress.let { ProjectAddress(it.city, it.houseNumber, it.street, it.zipCode) }

    return ProjectVersion(
        version = this.aggregateIdentifier.version,
        title = this.title,
        start = this.start.toLocalDateByMillis(),
        end = this.end.toLocalDateByMillis(),
        projectNumber = this.projectNumber,
        client = this.client,
        description = this.description,
        category = category,
        projectAddress = address,
        eventAuthor = auditUser,
        eventDate = auditDate)
  }
}
