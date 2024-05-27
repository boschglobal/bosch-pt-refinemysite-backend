/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.api.asUserIds
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.common.i18n.Key.TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT
import com.bosch.pt.iot.smartsite.project.attachment.model.dto.AttachmentDto
import com.bosch.pt.iot.smartsite.project.daycard.command.service.DayCardService
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.DAYCARD_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.participant.facade.rest.resource.response.referToWithPicture
import com.bosch.pt.iot.smartsite.project.participant.query.ParticipantQueryService
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.ProjectCraftResource
import com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response.factory.ProjectCraftResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isAcceptTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isAssignTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isCloseTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isDeletePossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isResetTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isSendTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isStartTaskPossible
import com.bosch.pt.iot.smartsite.project.task.command.handler.precondition.TaskPrecondition.isUnassignTaskPossible
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.EMBEDDED_TASK_ATTACHMENTS
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.EMBEDDED_TASK_CONSTRAINTS
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.EMBEDDED_TASK_SCHEDULE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.EMBEDDED_TASK_STATISTICS
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ACCEPT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_ASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CLOSE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CONSTRAINTS
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_CONSTRAINTS_UPDATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_DELETE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_RESET
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_SEND
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_START
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_SCHEDULE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TASK_UPDATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TOPIC
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_TOPIC_CREATE
import com.bosch.pt.iot.smartsite.project.task.facade.rest.resource.response.TaskResource.Companion.LINK_UNASSIGN
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ACCEPT_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.ASSIGN_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.CLOSE_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.PATH_VARIABLE_TASK_ID
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.RESET_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.SEND_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.START_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.facade.rest.util.TaskControllerUtils.UNASSIGN_TASK_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.taskattachment.boundary.TaskAttachmentQueryService
import com.bosch.pt.iot.smartsite.project.taskattachment.facade.rest.resource.response.factory.TaskAttachmentListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskconstraint.boundary.TaskConstraintSelectionService
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController.Companion.CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.TaskConstraintSelectionController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.TaskConstraintSelectionResource
import com.bosch.pt.iot.smartsite.project.taskconstraint.facade.rest.resource.response.factory.TaskConstraintSelectionListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULE_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_UPDATE_TASKSCHEDULE
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory.TaskScheduleResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.taskstatistics.boundary.TaskStatisticsService
import com.bosch.pt.iot.smartsite.project.taskstatistics.facade.rest.resource.response.TaskStatisticsResource
import com.bosch.pt.iot.smartsite.project.taskstatistics.model.TaskStatistics
import com.bosch.pt.iot.smartsite.project.topic.facade.rest.TopicController
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import datadog.trace.api.Trace
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TaskResourceFactoryHelper(
    messageSource: MessageSource,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val taskStatisticsService: TaskStatisticsService,
    private val participantQueryService: ParticipantQueryService,
    private val taskAttachmentQueryService: TaskAttachmentQueryService,
    private val taskConstraintSelectionService: TaskConstraintSelectionService,
    private val dayCardService: DayCardService,
    private val taskConstraintSelectionListResourceFactory:
        TaskConstraintSelectionListResourceFactory,
    private val taskAttachmentListResourceFactory: TaskAttachmentListResourceFactory,
    private val taskScheduleResourceFactoryHelper: TaskScheduleResourceFactoryHelper,
    private val projectCraftResourceFactoryHelper: ProjectCraftResourceFactoryHelper,
    private val userService: UserService,
    private val linkFactory: CustomLinkBuilderFactory
) : AbstractResourceFactoryHelper(messageSource) {

  @Trace
  @Transactional(readOnly = true)
  fun build(tasks: List<Task>, includeEmbedded: Boolean): List<TaskResource> {
    if (tasks.isEmpty()) {
      return emptyList()
    }
    assertTasksBelongToSameProject(tasks)

    val auditUsers = userService.findAuditUsers(tasks)

    val projectId = tasks.first().project.identifier
    val taskIds = tasks.map { it.identifier }.toSet()
    val creatorUserIds = tasks.map { it.createdBy.get().identifier }.toSet()

    // Collect authorization information
    val taskViewPermissions = taskAuthorizationComponent.filterTasksWithViewPermission(taskIds)
    val taskEditPermissions = taskAuthorizationComponent.filterTasksWithEditPermission(taskIds)
    val taskStatusChangePermissions =
        taskAuthorizationComponent.filterTasksWithStatusChangePermission(taskIds)
    val taskContributePermissions =
        taskAuthorizationComponent.filterTasksWithContributePermission(taskIds)
    val taskAssignPermissions = taskAuthorizationComponent.filterTasksWithAssignPermission(taskIds)
    val taskUnassignPermissions =
        taskAuthorizationComponent.filterTasksWithUnassignPermission(taskIds)
    val taskSendPermissions = taskAuthorizationComponent.filterTasksWithSendPermission(taskIds)
    val taskAcceptPermissions = taskAuthorizationComponent.filterTasksWithAcceptPermission(taskIds)
    val taskDeletePermissions = taskAuthorizationComponent.filterTasksWithDeletePermission(taskIds)

    // Extra information to add to the tasks
    val creatorParticipant =
        participantQueryService.findActiveAndInactiveParticipants(
            creatorUserIds.asUserIds(), setOf(projectId))
    val taskStatistics = taskStatisticsService.findTaskStatistics(taskIds)

    val taskSelections = taskConstraintSelectionService.findSelections(taskIds)
    val taskConstraintSelectionResourcesByTaskIdentifier =
        taskConstraintSelectionListResourceFactory
            .build(projectId, taskSelections)
            .selections
            .associateBy { it.taskIdentifier }

    val dayCardAggregationByTaskIdentifier = dayCardService.getStatusCountAggregation(taskIds)
    val attachmentsByTaskIdentifier =
        if (includeEmbedded) taskAttachmentQueryService.findAllAndMappedByTaskIdentifierIn(taskIds)
        else emptyMap()

    val projectCrafts = tasks.map { it.projectCraft }
    val projectCraftResourcesByIdentifier =
        projectCraftResourceFactoryHelper.build(projectCrafts).associateBy { it.id }

    val taskSchedules = tasks.filterNot { it.taskSchedule == null }.map { it.taskSchedule!! }
    val taskScheduleResourceByTaskIdentifier =
        taskScheduleResourceFactoryHelper.build(taskSchedules).associateBy { it.task.identifier }

    // Build of the task resource
    return tasks.map { task: Task ->
      val taskIdentifier = task.identifier
      val projectCraftIdentifier = task.projectCraft.identifier

      val allowedToDelete =
          (taskDeletePermissions.contains(taskIdentifier) &&
              isDeletePossible(dayCardAggregationByTaskIdentifier[taskIdentifier]))

      buildTaskResource(
          task = task,
          projectCraftResource =
              projectCraftResourcesByIdentifier[projectCraftIdentifier.toUuid()]!!,
          taskScheduleResource = taskScheduleResourceByTaskIdentifier[task.identifier.toUuid()],
          taskStatistics = taskStatistics[taskIdentifier],
          taskConstraintSelection =
              taskConstraintSelectionResourcesByTaskIdentifier[taskIdentifier.toUuid()],
          attachments = attachmentsByTaskIdentifier.getOrDefault(taskIdentifier, emptyList()),
          creator =
              creatorParticipant
                  .getOrDefault(
                      task.createdBy.map { it.identifier.asUserId() }.orElse(null), mutableMapOf())[
                      projectId],
          createdBy = auditUsers[task.createdBy.get()]!!,
          lastModifiedBy = auditUsers[task.lastModifiedBy.get()]!!,
          allowedToView = taskViewPermissions.contains(taskIdentifier),
          allowedToEdit = taskEditPermissions.contains(taskIdentifier),
          allowedToChangeStatus = taskStatusChangePermissions.contains(taskIdentifier),
          allowedToContribute = taskContributePermissions.contains(taskIdentifier),
          allowedToAssign = taskAssignPermissions.contains(taskIdentifier),
          allowedToUnassign = taskUnassignPermissions.contains(taskIdentifier),
          allowedToSend = taskSendPermissions.contains(taskIdentifier),
          allowedToAccept = taskAcceptPermissions.contains(taskIdentifier),
          allowedToDelete = allowedToDelete,
          includeEmbedded = includeEmbedded)
    }
  }

  private fun assertTasksBelongToSameProject(tasks: List<Task>) {
    if (tasks.distinctBy { it.project.identifier }.count() > 1) {
      throw PreconditionViolationException(TASK_VALIDATION_ERROR_NOT_OF_SAME_PROJECT)
    }
  }

  private fun buildTaskResource(
      task: Task,
      projectCraftResource: ProjectCraftResource,
      taskScheduleResource: TaskScheduleResource?,
      taskStatistics: TaskStatistics?,
      taskConstraintSelection: TaskConstraintSelectionResource?,
      attachments: Collection<AttachmentDto>,
      creator: Participant?,
      createdBy: User,
      lastModifiedBy: User,
      allowedToView: Boolean,
      allowedToEdit: Boolean,
      allowedToChangeStatus: Boolean,
      allowedToContribute: Boolean,
      allowedToAssign: Boolean,
      allowedToUnassign: Boolean,
      allowedToSend: Boolean,
      allowedToAccept: Boolean,
      allowedToDelete: Boolean,
      includeEmbedded: Boolean
  ): TaskResource {

    val resource =
        TaskResource(
            id = task.identifier.toUuid(),
            version = task.version,
            createdBy = referTo(createdBy, deletedUserReference)!!,
            createdDate = task.createdDate.get().toDate(),
            lastModifiedDate = task.lastModifiedDate.get().toDate(),
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            project = ResourceReference.from(task.project),
            name = task.name,
            description = task.description,
            location = task.location,
            projectCraft = projectCraftResource,
            workArea = task.workArea?.let { ResourceReference.from(it) },
            company = task.assignee?.company?.let { ResourceReference.from(it) },
            assignee = task.assignee?.referToWithPicture(deletedUserReference),
            creator = creator?.referToWithPicture(deletedUserReference),
            status = task.status,
            assigned = task.isAssigned(),
            editDate = task.editDate?.toDate())
    val taskIdentifier = task.identifier

    // Links depending on view permission of task, e.g. topics, constraints
    if (allowedToView) {
      allowedToViewTaskAttachments(taskIdentifier, attachments, resource, includeEmbedded)
      allowedToViewTaskSchedule(
          resource, taskScheduleResource, taskIdentifier, allowedToEdit, allowedToContribute)
      allowedToViewTaskConstraints(
          task.project.identifier, taskIdentifier, resource, taskConstraintSelection)
      allowedToViewStatistics(taskStatistics, resource)

      // Add topic links
      resource.add(
          linkFactory
              .linkTo(TopicController.TOPICS_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(TopicController.PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_TOPIC_CREATE))

      resource.add(
          linkFactory
              .linkTo(TopicController.TOPICS_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(TopicController.PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_TOPIC))
    }

    // Add update links if edit permission is true

    resource.addIf(allowedToEdit) {
      linkFactory
          .linkTo(TASK_BY_TASK_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
          .withRel(LINK_TASK_UPDATE)
    }

    // Add links depending on assign permission on task
    if (allowedToAssign) {
      allowedToAssignLinks(resource, taskIdentifier)
    }

    // Add links depending on unassign permission on task
    if (allowedToUnassign) {
      allowedToUnassignLinks(resource)
    }

    // Add links depending on send permission on task
    if (allowedToSend) {
      allowedToSendLinks(resource, taskIdentifier)
    }

    // Add links depending on status change permission on task
    if (allowedToChangeStatus) {
      allowedToChangeStatusLinks(resource, taskIdentifier)
    }

    // Add links depending on accept permission on task
    if (allowedToAccept) {
      allowedToAcceptLinks(resource, taskIdentifier)
    }

    // Add links depending on contribute permission on task
    if (allowedToContribute) {
      allowedToContributeLinks(resource, task.project.identifier, taskIdentifier)
    }

    // Add links depending on delete permission on task
    resource.addIf(allowedToDelete) {
      linkFactory
          .linkTo(TASK_BY_TASK_ID_ENDPOINT)
          .withParameters(
              mapOf(TaskConstraintSelectionController.PATH_VARIABLE_TASK_ID to taskIdentifier))
          .withRel(LINK_DELETE)
    }

    return resource
  }

  private fun allowedToViewStatistics(taskStatistics: TaskStatistics?, resource: TaskResource) =
      resource.apply {
        // Provides the "statistics" resource
        addResourceSupplier(EMBEDDED_TASK_STATISTICS) { TaskStatisticsResource(taskStatistics) }
        embed(EMBEDDED_TASK_STATISTICS)
      }

  private fun allowedToViewTaskConstraints(
      projectIdentifier: ProjectId,
      taskIdentifier: TaskId,
      resource: TaskResource,
      taskConstraintSelection: TaskConstraintSelectionResource?,
  ) {

    if (taskConstraintSelection != null) {
      // Provides the "task constraint selection" resource
      resource.addResourceSupplier(EMBEDDED_TASK_CONSTRAINTS) { taskConstraintSelection }

      resource.embed(EMBEDDED_TASK_CONSTRAINTS)
    }

    // Add link to the constraint selection to the task resource
    resource.add(
        linkFactory
            .linkTo(CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID)
            .withParameters(
                mapOf(
                    TaskConstraintSelectionController.PATH_VARIABLE_TASK_ID to taskIdentifier,
                    PATH_VARIABLE_PROJECT_ID to projectIdentifier))
            .withRel(LINK_CONSTRAINTS))
  }

  private fun allowedToViewTaskSchedule(
      resource: TaskResource,
      scheduleResource: TaskScheduleResource?,
      taskIdentifier: TaskId,
      allowedToEdit: Boolean,
      allowedToContribute: Boolean
  ) {

    if (scheduleResource != null) {
      // Provides the "schedule" resource
      resource.addResourceSupplier(EMBEDDED_TASK_SCHEDULE) {
        scheduleResource.addLinks(allowedToEdit, allowedToContribute)
        scheduleResource
      }

      resource.embed(EMBEDDED_TASK_SCHEDULE)
    }

    // Add link of the task schedule to the task resource
    resource.add(
        linkFactory
            .linkTo(SCHEDULE_BY_TASK_ID_ENDPOINT)
            .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
            .withRel(LINK_TASK_SCHEDULE))
  }

  private fun allowedToViewTaskAttachments(
      taskIdentifier: TaskId,
      attachments: Collection<AttachmentDto>,
      resource: TaskResource,
      includeEmbedded: Boolean
  ) {

    // Provides the "attachments" resource
    resource.addResourceSupplier(EMBEDDED_TASK_ATTACHMENTS) {
      taskAttachmentListResourceFactory.build(attachments, taskIdentifier)
    }

    if (includeEmbedded) {
      resource.embed(EMBEDDED_TASK_ATTACHMENTS)
    }
  }

  private fun allowedToAssignLinks(resource: TaskResource, taskIdentifier: TaskId) {
    // Add link to assign the task
    if (isAssignTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(ASSIGN_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_ASSIGN))
    }
  }

  private fun allowedToUnassignLinks(resource: TaskResource) {
    // Add link to unassign the task
    if (isUnassignTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(UNASSIGN_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to resource.id))
              .withRel(LINK_UNASSIGN))
    }
  }

  private fun allowedToSendLinks(resource: TaskResource, taskIdentifier: TaskId) {
    // Add link to send the task
    if (isSendTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(SEND_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_SEND))
    }
  }

  private fun allowedToAcceptLinks(resource: TaskResource, taskIdentifier: TaskId) {
    // Add link to accept the task
    if (isAcceptTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(ACCEPT_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_ACCEPT))
    }
  }

  private fun allowedToChangeStatusLinks(resource: TaskResource, taskIdentifier: TaskId) {

    // Add link to start the task
    if (isStartTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(START_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_START))
    }

    // Add link to close the task
    if (isCloseTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(CLOSE_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_CLOSE))
    }

    // Add link to reset the task
    if (isResetTaskPossible(resource.status)) {
      resource.add(
          linkFactory
              .linkTo(RESET_TASK_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to taskIdentifier))
              .withRel(LINK_RESET))
    }
  }

  private fun allowedToContributeLinks(
      resource: TaskResource,
      projectIdentifier: ProjectId,
      taskIdentifier: TaskId
  ) =
      resource.add(
          linkFactory
              .linkTo(CONSTRAINTS_BY_PROJECT_ID_AND_TASK_ID)
              .withParameters(
                  mapOf(
                      PATH_VARIABLE_TASK_ID to taskIdentifier,
                      PATH_VARIABLE_PROJECT_ID to projectIdentifier))
              .withRel(LINK_CONSTRAINTS_UPDATE))

  private fun TaskScheduleResource.addLinks(allowedToEdit: Boolean, allowedToContribute: Boolean) {
    addUpdateLink(allowedToEdit)
    addDayCardAddLink(allowedToContribute)
  }

  private fun TaskScheduleResource.addUpdateLink(allowedToEdit: Boolean) {
    if (allowedToEdit) {
      this.add(
          linkFactory
              .linkTo(SCHEDULE_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to task.identifier))
              .withRel(LINK_UPDATE_TASKSCHEDULE))
    }
  }

  private fun TaskScheduleResource.addDayCardAddLink(allowedToContribute: Boolean) {
    if (allowedToContribute) {
      this.add(
          linkFactory
              .linkTo(DAYCARD_BY_TASK_ID_ENDPOINT)
              .withParameters(mapOf(PATH_VARIABLE_TASK_ID to task.identifier))
              .withRel(LINK_CREATE_DAYCARD))
    }
  }
}
