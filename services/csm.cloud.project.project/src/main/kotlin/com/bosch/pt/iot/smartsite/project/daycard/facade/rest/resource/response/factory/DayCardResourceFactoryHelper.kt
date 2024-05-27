/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverageGenerated
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.NamedEnumReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.daycard.authorization.DayCardAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.daycard.command.service.precondition.DayCardPrecondition
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.APPROVE_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.CANCEL_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.COMPLETE_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.PATH_VARIABLE_DAY_CARD_ID
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.DayCardController.Companion.RESET_DAYCARD_BY_DAYCARD_ID_ENDPOINT
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_APPROVE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_CANCEL_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_COMPLETE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_DELETE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_RESET_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource.Companion.LINK_UPDATE_DAYCARD
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCard
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.dto.DayCardDto
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.rfv.boundary.RfvService
import com.bosch.pt.iot.smartsite.project.task.authorization.TaskAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.taskschedule.shared.model.dto.TaskScheduleSlotWithDayCardDto
import com.bosch.pt.iot.smartsite.user.boundary.UserService
import com.bosch.pt.iot.smartsite.user.model.User.Companion.referTo
import datadog.trace.api.Trace
import java.util.UUID
import java.util.function.Supplier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
open class DayCardResourceFactoryHelper(
    private val dayCardAuthorizationComponent: DayCardAuthorizationComponent,
    private val taskAuthorizationComponent: TaskAuthorizationComponent,
    private val rfvService: RfvService,
    private val linkFactory: CustomLinkBuilderFactory,
    private val userService: UserService,
    messageSource: MessageSource,
) : AbstractResourceFactoryHelper(messageSource) {

  @Trace
  open fun build(dayCards: List<DayCard>): List<DayCardResource> {
    if (dayCards.isEmpty()) {
      return emptyList()
    }

    val projectIdentifiers = dayCards.map { it.taskSchedule.task.project.identifier }.toSet()
    assertAllDayCardBelongToSameProject(projectIdentifiers)

    val taskIdentifiers = dayCards.map { it.taskSchedule.task.identifier }.toSet()

    val reviewPermission =
        dayCardAuthorizationComponent.hasReviewPermissionOnDayCardsOfProject(
            projectIdentifiers.first())

    val tasksWithContributePermission =
        taskAuthorizationComponent.filterTasksWithContributePermission(taskIdentifiers)

    val translatedRfvs = rfvService.resolveProjectRfvs(projectIdentifiers.first())

    return dayCards.map {
      buildFromEntity(
          translatedRfvs,
          it,
          tasksWithContributePermission.contains(it.taskSchedule.task.identifier),
          reviewPermission)
    }
  }

  @Trace
  open fun buildFromDtos(dayCards: List<DayCardDto>): List<DayCardResource> {
    if (dayCards.isEmpty()) {
      return emptyList()
    }

    val projectIdentifiers = dayCards.map { it.taskScheduleTaskProjectIdentifier }.toSet()
    assertAllDayCardBelongToSameProject(projectIdentifiers)

    val taskIdentifiers = dayCards.map { it.taskScheduleTaskIdentifier }.toSet()

    val reviewPermission =
        dayCardAuthorizationComponent.hasReviewPermissionOnDayCardsOfProject(
            projectIdentifiers.first())

    val tasksWithContributePermission =
        taskAuthorizationComponent.filterTasksWithContributePermission(taskIdentifiers)

    val translatedRfvs = rfvService.resolveProjectRfvs(projectIdentifiers.first())

    return dayCards.map {
      buildFromDto(
          translatedRfvs,
          it,
          tasksWithContributePermission.contains(it.taskScheduleTaskIdentifier),
          reviewPermission)
    }
  }

  private fun buildFromEntity(
      translatedRfvs: Map<DayCardReasonEnum, String>,
      dayCard: DayCard,
      contributeAndDeletePermission: Boolean,
      reviewPermission: Boolean
  ): DayCardResource {

    val auditUsers = userService.findAuditUsers(listOf(dayCard))

    val createdBy = auditUsers[dayCard.createdBy.get()]!!
    val lastModifiedBy = auditUsers[dayCard.lastModifiedBy.get()]!!

    // Create task resource references
    val task = dayCard.taskSchedule.task
    val taskResourceReference = ResourceReference(task.identifier.toUuid(), task.getDisplayName())
    return DayCardResource(
            id = dayCard.identifier.toUuid(),
            version = dayCard.version,
            createdDate = dayCard.createdDate.get().toDate(),
            lastModifiedDate = dayCard.lastModifiedDate.get().toDate(),
            createdBy = referTo(createdBy, deletedUserReference)!!,
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            task = taskResourceReference,
            title = dayCard.title,
            manpower = dayCard.manpower,
            notes = dayCard.notes,
            status = dayCard.status,
            reason = dayCard.reason?.let { NamedEnumReference(it, translatedRfvs[it]!!) })
        .apply { addLinks(reviewPermission, contributeAndDeletePermission) }
  }

  private fun buildFromDto(
      translatedRfvs: Map<DayCardReasonEnum, String>,
      dayCard: DayCardDto,
      contributeAndDeletePermission: Boolean,
      reviewPermission: Boolean
  ): DayCardResource {
    // Create user resource references

    val auditUsers = userService.findAuditUsersFromDayCardDtos(listOf(dayCard))

    val createdBy = auditUsers[dayCard.createdByIdentifier]!!
    val lastModifiedBy = auditUsers[dayCard.lastModifiedByIdentifier]!!

    // Create task resource references
    val taskResourceReference =
        ResourceReference(dayCard.taskScheduleTaskIdentifier.toUuid(), dayCard.taskScheduleTaskName)
    return DayCardResource(
            id = dayCard.identifier.toUuid(),
            version = dayCard.version,
            createdDate = dayCard.createdDate!!,
            lastModifiedDate = dayCard.lastModifiedDate!!,
            createdBy = referTo(createdBy, deletedUserReference)!!,
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            task = taskResourceReference,
            title = dayCard.title,
            manpower = dayCard.manpower,
            notes = dayCard.notes,
            status = dayCard.status,
            reason = dayCard.reason?.let { NamedEnumReference(it, translatedRfvs[it]!!) })
        .apply { addLinks(reviewPermission, contributeAndDeletePermission) }
  }

  fun build(
      scheduleDayCard: TaskScheduleSlotWithDayCardDto,
      taskIdentifier: UUID,
      taskName: String,
      deletedUserReference: Supplier<ResourceReference>,
      translatedRfvs: Map<DayCardReasonEnum, String>,
      allowedToReview: Boolean,
      allowedToContribute: Boolean,
      userService: UserService
  ): DayCardResource {

    // Create user resource references
    val auditUsers =
        userService.findAuditUsersFromTaskScheduleSlotWithDayCardDto(listOf(scheduleDayCard))

    val createdBy = auditUsers[scheduleDayCard.slotsDayCardCreatedBy]!!
    val lastModifiedBy = auditUsers[scheduleDayCard.slotsDayCardLastModifiedBy]!!

    // Create task resource references
    val taskResourceReference = ResourceReference(taskIdentifier, taskName)

    return DayCardResource(
            id = scheduleDayCard.slotsDayCardIdentifier.identifier,
            version = scheduleDayCard.slotsDayCardVersion,
            createdDate = scheduleDayCard.slotsDayCardCreatedDate!!,
            lastModifiedDate = scheduleDayCard.slotsDayCardLastModifiedDate!!,
            createdBy = referTo(createdBy, deletedUserReference)!!,
            lastModifiedBy = referTo(lastModifiedBy, deletedUserReference)!!,
            task = taskResourceReference,
            title = scheduleDayCard.slotsDayCardTitle,
            manpower = scheduleDayCard.slotsDayCardManpower,
            notes = scheduleDayCard.slotsDayCardNotes,
            status = scheduleDayCard.slotsDayCardStatus,
            reason =
                scheduleDayCard.slotsDayCardReason?.let {
                  NamedEnumReference(it, translatedRfvs[it]!!)
                })
        .apply { addLinks(allowedToReview, allowedToContribute) }
  }

  protected open fun DayCardResource.addLinks(
      allowedToReview: Boolean,
      allowedToContribute: Boolean
  ) {
    // add update link
    addIf(DayCardPrecondition.isEditPossible(status) && allowedToContribute) {
      linkFactory
          .linkTo(DAYCARD_BY_DAYCARD_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_DAY_CARD_ID to id))
          .withRel(LINK_UPDATE_DAYCARD)
    }

    // add cancel link
    addIf(DayCardPrecondition.isCancelPossible(status, allowedToReview) && allowedToContribute) {
      linkFactory
          .linkTo(CANCEL_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_DAY_CARD_ID to id))
          .withRel(LINK_CANCEL_DAYCARD)
    }

    // add complete link
    addIf(DayCardPrecondition.isCompletePossible(status) && allowedToContribute) {
      linkFactory
          .linkTo(COMPLETE_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_DAY_CARD_ID to id))
          .withRel(LINK_COMPLETE_DAYCARD)
    }

    // add approve link
    addIf(DayCardPrecondition.isApprovePossible(status) && allowedToReview && allowedToContribute) {
      linkFactory
          .linkTo(APPROVE_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_DAY_CARD_ID to id))
          .withRel(LINK_APPROVE_DAYCARD)
    }

    // add reset link
    addIf(DayCardPrecondition.isResetPossible(status) && allowedToReview && allowedToContribute) {
      linkFactory
          .linkTo(RESET_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_DAY_CARD_ID to id))
          .withRel(LINK_RESET_DAYCARD)
    }

    // add delete link
    addIf(DayCardPrecondition.isDeletePossible(status) && allowedToContribute) {
      linkFactory
          .linkTo(DAYCARD_BY_DAYCARD_ID_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_DAY_CARD_ID to id))
          .withRel(LINK_DELETE_DAYCARD)
    }
  }

  @ExcludeFromCodeCoverageGenerated
  private fun assertAllDayCardBelongToSameProject(projectIdentifiers: Set<ProjectId>) =
      require(projectIdentifiers.size == 1) {
        "Day Card Resource Factory Helper was called for Day Cards of different projects."
      }
}
