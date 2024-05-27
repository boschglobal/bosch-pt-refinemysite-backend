/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.exporter.boundary

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventEnumAvro
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.application.security.SecurityContextHelper
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureEnum.PROJECT_EXPORT
import com.bosch.pt.iot.smartsite.featuretoggle.query.FeatureQueryService
import com.bosch.pt.iot.smartsite.project.daycard.shared.repository.DayCardRepository
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.PRIMAVERA_P6_XML
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportParameters
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.Description
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.MessageAttachmentDescription
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.MessageDescription
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.TopicAttachmentDescription
import com.bosch.pt.iot.smartsite.project.exporter.model.dto.TopicDescription
import com.bosch.pt.iot.smartsite.project.exporter.model.tree.Tree
import com.bosch.pt.iot.smartsite.project.exporter.model.tree.Tree.Companion.getExternalIdType
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.external.repository.ExternalIdRepository
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.MSPDI
import com.bosch.pt.iot.smartsite.project.importer.boundary.SupportedFileTypes.PMXML
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import com.bosch.pt.iot.smartsite.project.messageattachment.model.MessageAttachment
import com.bosch.pt.iot.smartsite.project.messageattachment.repository.MessageAttachmentRepository
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone
import com.bosch.pt.iot.smartsite.project.milestone.shared.repository.MilestoneListRepository
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElementTypeEnum.MILESTONE
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum.FINISH_TO_START
import com.bosch.pt.iot.smartsite.project.relation.repository.RelationRepository
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.task.shared.repository.TaskRepository
import com.bosch.pt.iot.smartsite.project.topic.shared.model.dto.TopicDto
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import com.bosch.pt.iot.smartsite.project.topicattachment.model.TopicAttachment
import com.bosch.pt.iot.smartsite.project.topicattachment.repository.TopicAttachmentRepository
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import com.bosch.pt.iot.smartsite.project.workarea.shared.repository.WorkAreaListRepository
import com.bosch.pt.iot.smartsite.project.workday.shared.model.WorkdayConfiguration
import com.bosch.pt.iot.smartsite.project.workday.shared.repository.WorkdayConfigurationRepository
import datadog.trace.api.Trace
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDateTime
import net.sf.mpxj.ProjectCalendar
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_AFTERNOON
import net.sf.mpxj.ProjectCalendarDays.DEFAULT_WORKING_MORNING
import net.sf.mpxj.ProjectFile
import net.sf.mpxj.mspdi.MSPDIWriter
import net.sf.mpxj.primavera.PrimaveraPMFileWriter
import org.springframework.context.MessageSource
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectExportService(
    private val dayCardRepository: DayCardRepository,
    private val featureQueryService: FeatureQueryService,
    private val externalIdRepository: ExternalIdRepository,
    private val messageAttachmentRepository: MessageAttachmentRepository,
    private val messageRepository: MessageRepository,
    private val messageSource: MessageSource,
    private val milestoneListRepository: MilestoneListRepository,
    private val relationRepository: RelationRepository,
    private val taskRepository: TaskRepository,
    private val topicAttachmentRepository: TopicAttachmentRepository,
    private val topicRepository: TopicRepository,
    private val workdayConfigurationRepository: WorkdayConfigurationRepository,
    private val workAreaListRepository: WorkAreaListRepository
) {

  @Trace
  @Transactional(readOnly = true)
  @NoPreAuthorize
  fun isExportPossible(project: Project): Boolean =
      featureQueryService.isFeatureEnabled(PROJECT_EXPORT, project.identifier)

  @Trace
  @Transactional
  @PreAuthorize("@projectAuthorizationComponent.hasReadPermissionOnProject(#project.identifier)")
  fun export(project: Project, projectExportParameters: ProjectExportParameters): ByteArray {

    val startHour = DEFAULT_WORKING_MORNING.start.hour.toLong()
    val endHour = DEFAULT_WORKING_AFTERNOON.end.hour.toLong()

    val workDayConfiguration =
        workdayConfigurationRepository.findOneWithDetailsByProjectIdentifier(project.identifier)

    val projectFile =
        ProjectFile().apply {

          // Set basic parameters of the project
          projectProperties.projectTitle = project.title
          projectProperties.projectID = project.title
          projectProperties.plannedStart = project.start.atStartOfDay().plusHours(startHour)
          projectProperties.scheduledFinish = project.end.atStartOfDay().plusHours(endHour)
          projectProperties.author =
              SecurityContextHelper.getInstance().getCurrentUser().getDisplayName()
          projectProperties.fileType =
              when (projectExportParameters.format) {
                MS_PROJECT_XML -> MSPDI.name
                PRIMAVERA_P6_XML -> PMXML.name
              }

          initializeWorkingDaysAndHours(workDayConfiguration)
        }

    val externalIdType = getExternalIdType(projectFile)
    val originalExternalIds = getExternalIds(project.identifier, externalIdType)
    val tree =
        Tree(
            projectFile = projectFile,
            project = project,
            existingExternalIds = originalExternalIds,
            idType = externalIdType,
            allowWorkOnNonWorkingDays = workDayConfiguration?.allowWorkOnNonWorkingDays ?: false,
            requestedTaskSchedulingType = projectExportParameters.taskExportSchedulingType,
            requestedMilestoneSchedulingType =
                projectExportParameters.milestoneExportSchedulingType,
        )

    // Add work areas
    getWorkAreas(project.identifier).forEach { tree.addWorkArea(it) }

    // Add tasks with nested elements
    val tasks = getTasks(project.identifier)
    val taskIds = tasks.map { checkNotNull(it.identifier) }
    val taskDescriptions: Map<TaskId, List<Description>> =
        if (projectExportParameters.includeComments) {
          getTopicDescriptions(taskIds).also {
            addTopicAttachmentsToTopicDescriptions(taskIds, it)
            addMessagesToTopicDescriptions(taskIds, it)
            addMessageAttachmentsToTopicDescriptions(taskIds, it)
          }
        } else emptyMap()
    val dayCardsByTaskId =
        dayCardRepository.findAllByTaskScheduleProjectIdentifier(project.identifier).groupBy {
          checkNotNull(checkNotNull(checkNotNull(it.taskSchedule).task).identifier)
        }

    tasks.forEach { task ->
      tree.addTask(
          task,
          taskDescriptions[checkNotNull(task.identifier)] ?: emptyList(),
          dayCardsByTaskId[checkNotNull(task.identifier)] ?: emptyList())
    }

    // Add milestones
    getMilestones(projectExportParameters.includeMilestones, project.identifier).forEach {
      tree.addMilestone(it)
    }

    // Add relations
    getRelations(projectExportParameters.includeMilestones, project.identifier).forEach {
      tree.addRelation(it)
    }

    // Save external IDs in the database and kafka
    val externalIds = tree.write(messageSource)
    val externalObjectIds = externalIds.map { it.objectIdentifier }

    val originalExternalObjectIds = originalExternalIds.map { it.objectIdentifier }
    val removedObjectIds = originalExternalObjectIds - externalObjectIds.toSet()
    val deletedExternalIds =
        originalExternalIds.filter { removedObjectIds.contains(it.objectIdentifier) }

    val updatedExternalIds = externalIds.filter { !it.isNew && !deletedExternalIds.contains(it) }
    val newExternalIds = externalIds.filter { it.isNew }

    deletedExternalIds.forEach { externalIdRepository.delete(it, ExternalIdEventEnumAvro.DELETED) }
    // Hibernate seem to be smart enough to detect if ExternalId entities have been changed or not
    updatedExternalIds.forEach { externalIdRepository.save(it, ExternalIdEventEnumAvro.UPDATED) }
    newExternalIds.forEach { externalIdRepository.save(it, ExternalIdEventEnumAvro.CREATED) }

    // Sort tasks and working areas into the correct order, as this is required by MS Project to
    // display them at the correct location in the table and Gantt-Chart.
    projectFile.tasks.synchronizeTaskIDToHierarchy()

    // Generate XML format
    return ByteArrayOutputStream()
        .apply { getProjectWriter(projectExportParameters.format).write(projectFile, this) }
        .toByteArray()
  }

  fun getProjectWriter(format: ProjectExportFormatEnum) =
      when (format) {
        MS_PROJECT_XML -> MSPDIWriter()
        PRIMAVERA_P6_XML -> PrimaveraPMFileWriter()
      }

  private fun ProjectFile.initializeWorkingDaysAndHours(
      workdayConfiguration: WorkdayConfiguration?
  ) {

    val calendar = addDefaultBaseCalendar().apply { defaultCalendar = this }
    if (workdayConfiguration == null) {

      // Set each day is a working day as default if no configuration is defined
      DayOfWeek.values()
          .filterNot { listOf(SATURDAY, SUNDAY).contains(it) }
          .forEach { day ->
            calendar.setWorkingDay(day, true)

            // Add default working hours at the weekend to ensure that scheduled tasks are displayed
            // at the correct location (otherwise MS Project shifts / enlarges them).
            addRegularWorkingHours(calendar, day)
          }

      // Set beginning of week to monday
      projectProperties.weekStartDay = MONDAY
    } else {
      val workDays = workdayConfiguration.workingDays.map { it }

      DayOfWeek.values().map { day ->
        val isWorkDay = workDays.contains(day)
        calendar.setWorkingDay(day, isWorkDay)
        if (isWorkDay) {

          // Add default working hours at the weekend to ensure that scheduled tasks are displayed
          // at the correct location (otherwise MS Project shifts / enlarges them).
          addRegularWorkingHours(calendar, day)
        }
      }

      workdayConfiguration.holidays.map { holiday ->
        calendar.addCalendarException(holiday.date).name = holiday.name
      }

      projectProperties.weekStartDay = workdayConfiguration.startOfWeek
    }
  }

  /**
   * Add default working hours at the weekend to ensure that scheduled tasks are displayed at the
   * correct location (otherwise MS Project shifts / enlarges them).
   */
  private fun addRegularWorkingHours(calendar: ProjectCalendar, day: DayOfWeek) {
    val hours = calendar.getCalendarHours(day)
    if (hours.isEmpty()) {
      hours.add(DEFAULT_WORKING_MORNING)
      hours.add(DEFAULT_WORKING_AFTERNOON)
    }
  }

  private fun getTasks(projectId: ProjectId): List<Task> =
      taskRepository.findAllByProjectIdentifier(projectId, Pageable.unpaged()).content

  private fun addMessageAttachmentsToTopicDescriptions(
      taskIds: List<TaskId>,
      descriptions: Map<TaskId, List<Description>>
  ) =
      messageAttachmentRepository
          .findAllByTaskIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse<MessageAttachment>(
              taskIds)
          .forEach { messageAttachment ->
            descriptions[checkNotNull(messageAttachment.task?.identifier)]
                ?.single {
                  it is TopicDescription &&
                      it.identifier == checkNotNull(messageAttachment.topic).identifier
                }
                .apply {
                  with(this as TopicDescription) {
                    this.children.add(
                        MessageAttachmentDescription(
                            messageAttachment.fileName!!,
                            messageAttachment.createdDate.orElse(LocalDateTime.now())))
                  }
                }
          }

  private fun addMessagesToTopicDescriptions(
      taskIds: List<TaskId>,
      descriptions: Map<TaskId, List<Description>>
  ) =
      messageRepository
          .findAllWithDetailsByTopicTaskIdentifierIn(taskIds, Pageable.unpaged())
          .content
          .forEach { message ->
            descriptions[checkNotNull(message.topic?.task?.identifier)]
                ?.single {
                  it is TopicDescription && it.identifier == checkNotNull(message.topic).identifier
                }
                .apply {
                  with(this as TopicDescription) {
                    this.children.add(
                        MessageDescription(
                            message.content, message.createdDate.orElse(LocalDateTime.now())))
                  }
                }
          }

  private fun addTopicAttachmentsToTopicDescriptions(
      taskIds: List<TaskId>,
      descriptions: Map<TaskId, List<Description>>
  ) =
      topicAttachmentRepository
          .findAllByTaskIdentifierInAndTaskDeletedFalseAndTopicDeletedFalse<TopicAttachment>(
              taskIds)
          .forEach { topicAttachment ->
            descriptions[checkNotNull(topicAttachment.task?.identifier)]
                ?.single {
                  it is TopicDescription &&
                      it.identifier == checkNotNull(topicAttachment.topic).identifier
                }
                .apply {
                  with(this as TopicDescription) {
                    this.children.add(
                        TopicAttachmentDescription(
                            topicAttachment.fileName!!,
                            topicAttachment.createdDate.orElse(LocalDateTime.now())))
                  }
                }
          }

  private fun getTopicDescriptions(taskIds: List<TaskId>): Map<TaskId, List<Description>> =
      topicRepository
          .findAllByTaskIdentifierInAndDeletedFalse(
              taskIds, Pageable.unpaged(), TopicDto::class.java)
          .groupBy { it.taskIdentifier }
          .mapValues { (_, topicDtos) ->
            topicDtos.map { topic ->
              TopicDescription(
                  topic.identifier,
                  topic.description,
                  topic.createdDate.toInstant().toEpochMilli().toLocalDateTimeByMillis(),
                  mutableListOf())
            }
          }

  private fun getMilestones(includeMilestones: Boolean, projectId: ProjectId): List<Milestone> =
      if (includeMilestones) {
        milestoneListRepository
            .findAllWithDetailsByProjectIdentifier(projectId)
            .map { it.milestones }
            .flatten()
      } else emptyList()

  private fun getWorkAreas(projectId: ProjectId): List<WorkArea> =
      workAreaListRepository.findOneWithDetailsByProjectIdentifier(projectId)?.workAreas
          ?: emptyList()

  private fun getRelations(includeMilestones: Boolean, projectId: ProjectId): List<Relation> =
      relationRepository
          .findAllByProjectIdentifier(projectId)
          .filter { it.type == FINISH_TO_START }
          .filter {
            includeMilestones || (it.source.type != MILESTONE && it.target.type != MILESTONE)
          }

  private fun getExternalIds(
      projectId: ProjectId,
      externalIdType: ExternalIdType
  ): List<ExternalId> = externalIdRepository.findAllByProjectIdAndIdType(projectId, externalIdType)

  companion object {
    const val FIELD_ALIAS_CRAFT = "Discipline"
  }
}
