/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.daycard.authorization.DayCardAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.DAYCARD_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory.DayCardResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.TaskScheduleController.Companion.SCHEDULE_BY_TASK_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.EMBEDDED_DAYCARDS_SCHEDULE
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_CREATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource.Companion.LINK_UPDATE_TASKSCHEDULE
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleSlotResource
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleWithDayCardsDto
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class TaskScheduleListResourceFactoryHelper(
    private val dayCardResourceFactory: DayCardResourceFactoryHelper,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val dayCardAuthorizationComponent: DayCardAuthorizationComponent,
    private val rfvService: RfvService,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  @Trace
  @Transactional(readOnly = true)
  open fun build(schedules: Collection<TaskScheduleWithDayCardsDto>): List<TaskScheduleResource> {
    if (schedules.isEmpty()) {
      return emptyList()
    }

    val taskIdentifiers = schedules.map { it.taskIdentifier }.toSet()
    val projectIdentifiers = schedules.map { it.taskProjectIdentifier }.toSet()

    require(projectIdentifiers.size == 1) {
      "Task Schedule Factory was called for different projects."
    }

    // Collect all create and modifier by user identifiers
    val auditUsers = findAuditUsers(schedules)

    val editPermissions = taskAuthorizationComponent.filterTasksWithEditPermission(taskIdentifiers)
    val contributePermissions =
        taskAuthorizationComponent.filterTasksWithContributePermission(taskIdentifiers)
    val allowedToReviewDayCard =
        dayCardAuthorizationComponent.hasReviewPermissionOnDayCardsOfProject(
            projectIdentifiers.first())

    val translatedRfvs = rfvService.resolveProjectRfvs(projectIdentifiers.first())

    return schedules.map { schedule ->
      val taskIdentifier = schedule.taskIdentifier

      return@map buildTaskScheduleResource(
          schedule = schedule,
          allowedToReviewDayCard = allowedToReviewDayCard,
          allowedToEdit = editPermissions.contains(taskIdentifier),
          allowedToContribute = contributePermissions.contains(taskIdentifier),
          translatedRfvs = translatedRfvs,
          auditUsers)
    }
  }

  private fun buildTaskScheduleResource(
      schedule: TaskScheduleWithDayCardsDto,
      allowedToReviewDayCard: Boolean,
      allowedToEdit: Boolean,
      allowedToContribute: Boolean,
      translatedRfvs: Map<DayCardReasonEnum, String>,
      auditUsers: Map<UserId, User>
  ): TaskScheduleResource {
    // Create the collection os task schedule slot resources
    val taskScheduleSlotResources = assembleSlotResources(schedule.scheduleSlotsWithDayCards)

    // Create task resource references
    val taskResourceReference =
        ResourceReference(schedule.taskIdentifier.toUuid(), schedule.taskName)
    return TaskScheduleResource(
            id = schedule.identifier.toUuid(),
            version = schedule.version,
            createdDate = schedule.createdDate!!,
            createdBy = referTo(auditUsers[schedule.createdByIdentifier]!!, deletedUserReference)!!,
            lastModifiedBy =
                referTo(auditUsers[schedule.lastModifiedByIdentifier]!!, deletedUserReference)!!,
            lastModifiedDate = schedule.lastModifiedDate!!,
            start = schedule.start,
            end = schedule.end,
            task = taskResourceReference,
            slots = taskScheduleSlotResources)
        .apply {
          addLinks(allowedToEdit, allowedToContribute)

          // Provides the "dayCards" resource
          addResourceSupplier(EMBEDDED_DAYCARDS_SCHEDULE) {
            buildDayCardListResource(
                schedule, allowedToReviewDayCard, allowedToContribute, translatedRfvs)
          }
          embed(EMBEDDED_DAYCARDS_SCHEDULE)
        }
  }

  private fun buildDayCardListResource(
      schedule: TaskScheduleWithDayCardsDto,
      allowedToReview: Boolean,
      allowedToContribute: Boolean,
      translatedRfvs: Map<DayCardReasonEnum, String>
  ): BatchResponseResource<DayCardResource> =
      BatchResponseResource(
          schedule.scheduleSlotsWithDayCards.map {
            buildDayCardResource(
                scheduleDayCard = it,
                taskIdentifier = schedule.taskIdentifier.toUuid(),
                taskName = schedule.taskName,
                allowedToReview = allowedToReview,
                allowedToContribute = allowedToContribute,
                translatedRfvs = translatedRfvs)
          })

  private fun buildDayCardResource(
      scheduleDayCard: TaskScheduleSlotWithDayCardDto,
      taskIdentifier: UUID,
      taskName: String,
      allowedToReview: Boolean,
      allowedToContribute: Boolean,
      translatedRfvs: Map<DayCardReasonEnum, String>
  ): DayCardResource =
      dayCardResourceFactory.build(
          scheduleDayCard = scheduleDayCard,
          taskIdentifier = taskIdentifier,
          taskName = taskName,
          deletedUserReference = deletedUserReference,
          translatedRfvs = translatedRfvs,
          allowedToReview = allowedToReview,
          allowedToContribute = allowedToContribute,
          userService = userService)

  private fun assembleSlotResources(
      scheduleDayCards: Collection<TaskScheduleSlotWithDayCardDto>
  ): Collection<TaskScheduleSlotResource> = scheduleDayCards.map { assembleSlotResource(it) }

  private fun assembleSlotResource(
      scheduleDayCard: TaskScheduleSlotWithDayCardDto
  ): TaskScheduleSlotResource =
      TaskScheduleSlotResource(
          ResourceReference(
              scheduleDayCard.slotsDayCardIdentifier.toUuid(), scheduleDayCard.slotsDayCardTitle),
          scheduleDayCard.slotsDate)

  private fun TaskScheduleResource.addLinks(allowedToEdit: Boolean, allowedToContribute: Boolean) {
    addUpdateLink(allowedToEdit)
    addDayCardAddLink(allowedToContribute)
  }

  private fun TaskScheduleResource.addUpdateLink(allowedToEdit: Boolean) {
    addIf(allowedToEdit) {
      linkFactory
          .linkTo(SCHEDULE_BY_TASK_ID_ENDPOINT)
          .withParameters(mapOf(TaskScheduleController.PATH_VARIABLE_TASK_ID to task.identifier))
          .withRel(LINK_UPDATE_TASKSCHEDULE)
    }
  }

  private fun TaskScheduleResource.addDayCardAddLink(allowedToContribute: Boolean) {
    addIf(allowedToContribute) {
      linkFactory
          .linkTo(DAYCARD_BY_TASK_ID_ENDPOINT)
          .withParameters(mapOf(DayCardController.PATH_VARIABLE_TASK_ID to task.identifier))
          .withRel(LINK_CREATE_DAYCARD)
    }
  }

  private fun findAuditUsers(
      schedules: Collection<TaskScheduleWithDayCardsDto>
  ): Map<UserId, User> {
    val userIdentifiers =
        schedules
            .flatMap {
              setOf(it.createdByIdentifier!!.identifier, it.lastModifiedByIdentifier!!.identifier)
            }
            .toSet()

    return userService.findAll(userIdentifiers).associateBy { it.identifier!!.asUserId() }
  }
}
