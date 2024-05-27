/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.daycard.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils
import com.bosch.pt.csm.cloud.common.facade.rest.etag.ETag
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.VersionedUpdateBatchRequestResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.iot.smartsite.common.exceptions.BatchIdentifierTypeNotSupportedException
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.etag.toEtagString
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.request.BatchRequestIdentifierType.DAYCARD
import com.bosch.pt.iot.smartsite.common.i18n.Key.COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED
import com.bosch.pt.iot.smartsite.project.daycard.command.api.ApproveDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CancelDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CompleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.CreateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.DeleteDayCardsFromScheduleCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.ResetDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.api.UpdateDayCardCommand
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.CreateDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.DeleteDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.UpdateDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.DeleteDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.ApproveDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.CancelDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.CompleteDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.batch.status.ResetDayCardBatchCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.ApproveDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.CancelDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.CompleteDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.command.handler.status.ResetDayCardCommandHandler
import com.bosch.pt.iot.smartsite.project.daycard.domain.DayCardId
import com.bosch.pt.iot.smartsite.project.daycard.domain.asDayCardIds
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.CancelDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.CancelMultipleDayCardsResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.DeleteMultipleDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.SaveDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.request.UpdateDayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.DayCardResource
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory.DayCardBatchResourceFactory
import com.bosch.pt.iot.smartsite.project.daycard.facade.rest.resource.response.factory.DayCardResourceFactory
import com.bosch.pt.iot.smartsite.project.daycard.query.DayCardQueryService
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.command.api.AddDayCardsToTaskScheduleCommand
import com.bosch.pt.iot.smartsite.project.taskschedule.command.handler.daycard.AddDayCardToTaskScheduleCommandHandler
import com.bosch.pt.iot.smartsite.project.taskschedule.command.service.TaskScheduleService
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.TaskScheduleResource
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory.TaskScheduleListResourceFactory
import com.bosch.pt.iot.smartsite.project.taskschedule.facade.rest.resource.response.factory.TaskScheduleResourceFactory
import com.bosch.pt.iot.smartsite.project.taskschedule.query.TaskScheduleQueryService
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import jakarta.validation.Valid
import org.springframework.dao.CannotAcquireLockException
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion
@Validated
@RestController
open class DayCardController(
    private val dayCardResourceFactory: DayCardResourceFactory,
    private val dayCardBatchResourceFactory: DayCardBatchResourceFactory,
    private val updateDayCardCommandHandler: UpdateDayCardCommandHandler,
    private val createDayCardCommandHandler: CreateDayCardCommandHandler,
    private val approveDayCardCommandHandler: ApproveDayCardCommandHandler,
    private val resetDayCardCommandHandler: ResetDayCardCommandHandler,
    private val resetDayCardBatchCommandHandler: ResetDayCardBatchCommandHandler,
    private val approveDayCardBatchCommandHandler: ApproveDayCardBatchCommandHandler,
    private val deleteDayCardCommandHandler: DeleteDayCardCommandHandler,
    private val deleteDayCardBatchCommandHandler: DeleteDayCardBatchCommandHandler,
    private val cancelDayCardCommandHandler: CancelDayCardCommandHandler,
    private val completeDayCardCommandHandler: CompleteDayCardCommandHandler,
    private val cancelDayCardBatchCommandHandler: CancelDayCardBatchCommandHandler,
    private val completeDayCardBatchCommandHandler: CompleteDayCardBatchCommandHandler,
    private val dayCardQueryService: DayCardQueryService,
    private val addDayCardToTaskScheduleCommandHandler: AddDayCardToTaskScheduleCommandHandler,
    private val taskScheduleQueryService: TaskScheduleQueryService,
    private val taskScheduleService: TaskScheduleService,
    private val taskScheduleResourceFactory: TaskScheduleResourceFactory,
    private val taskScheduleListResourceFactory: TaskScheduleListResourceFactory,
    private val transactionTemplate: TransactionTemplate,
) {

  @Retryable(
      CannotAcquireLockException::class,
      maxAttempts = 5,
      backoff = Backoff(multiplier = 1.5, random = true))
  @PostMapping(DAYCARD_BY_TASK_ID_ENDPOINT, DAYCARD_BY_TASK_ID_AND_DAYCARD_ID_ENDPOINT)
  open fun addDayCardToTaskSchedule(
      @PathVariable(PATH_VARIABLE_TASK_ID) taskIdentifier: TaskId,
      @PathVariable(name = PATH_VARIABLE_DAY_CARD_ID, required = false)
      dayCardIdentifier: DayCardId?,
      @RequestBody @Valid saveDayCardResource: SaveDayCardResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") scheduleETag: ETag
  ): ResponseEntity<TaskScheduleResource> {

    // Create day card and "add day card to schedule" in a single transaction to avoid dangling day
    // cards. Otherwise, if the day card creation succeeds but adding it to the schedule fails, we
    // are left with a day card that isn't part of any schedule.
    val createdDayCardIdentifier =
        transactionTemplate.execute {
          val taskSchedule = taskScheduleService.findByTaskIdentifier(taskIdentifier)

          val createdIdentifier =
              createDayCardCommandHandler.handle(
                  CreateDayCardCommand(
                      identifier = dayCardIdentifier ?: DayCardId(),
                      taskIdentifier = taskIdentifier,
                      title = saveDayCardResource.title,
                      manpower = saveDayCardResource.manpower,
                      notes = saveDayCardResource.notes))

          // Note: after the daycard has been created, the transaction also holds a shared lock
          // on the referenced schedule due to the foreign key constraint. Below, to add the day
          // card to the schedule, this lock needs to be upgraded to an exclusive lock. If in
          // the meantime another transaction acquires a shared lock on the same schedule, or
          // tries to get an exclusive lock, both cases can lead to a deadlock.
          addDayCardToTaskScheduleCommandHandler.handle(
              AddDayCardsToTaskScheduleCommand(
                  taskScheduleIdentifier = taskSchedule.identifier,
                  projectIdentifier = taskSchedule.project.identifier,
                  taskIdentifier = taskIdentifier,
                  dayCardIdentifier = createdIdentifier,
                  date = saveDayCardResource.date,
                  eTag = scheduleETag))

          createdIdentifier
        }

    val schedule =
        taskScheduleQueryService.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskIdentifier)
    val location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(LinkUtils.getCurrentApiVersionPrefix())
            .path(DAYCARD_BY_TASK_ID_AND_DAYCARD_ID_ENDPOINT)
            .buildAndExpand(taskIdentifier, createdDayCardIdentifier)
            .toUri()

    return ResponseEntity.created(location)
        .eTag(schedule.version.toString())
        .body(taskScheduleResourceFactory.build(schedule))
  }

  @GetMapping(DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun findByDayCardIdentifier(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardIdentifier: DayCardId
  ): ResponseEntity<DayCardResource> {
    val dayCard = dayCardQueryService.findWithDetails(dayCardIdentifier)
    return ResponseEntity.ok()
        .eTag(dayCard.toEtagString())
        .body(dayCardResourceFactory.build(dayCard))
  }

  @PostMapping(DAYCARDS_ENDPOINT)
  open fun findAllByDayCardIdentifiers(
      @RequestBody @Valid batchRequestResource: BatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String
  ): ResponseEntity<BatchResponseResource<DayCardResource>> =
      if (identifierType == DAYCARD) {
        ResponseEntity.ok()
            .body(
                dayCardBatchResourceFactory.build(
                    dayCardQueryService.findAllByIdentifierIn(
                        batchRequestResource.ids.asDayCardIds())))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @PutMapping(DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun updateDayCard(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardIdentifier: DayCardId,
      @RequestBody @Valid updateDayCardResource: UpdateDayCardResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") dayCardETag: ETag
  ): ResponseEntity<DayCardResource> {
    updateDayCardCommandHandler.handle(
        UpdateDayCardCommand(
            identifier = dayCardIdentifier,
            title = updateDayCardResource.title,
            manpower = updateDayCardResource.manpower,
            notes = updateDayCardResource.notes,
            dayCardETag))

    val dayCard = dayCardQueryService.findWithDetails(dayCardIdentifier)

    return ResponseEntity.ok()
        .eTag(dayCard.toEtagString())
        .body(dayCardResourceFactory.build(dayCard))
  }

  @DeleteMapping(DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun deleteDayCardAndSlot(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardId: DayCardId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") scheduleETag: ETag
  ): ResponseEntity<TaskScheduleResource> {
    val taskIdentifier = dayCardQueryService.findTaskIdentifier(dayCardId)!!
    deleteDayCardCommandHandler.handle(DeleteDayCardCommand(dayCardId, scheduleETag))

    val schedule =
        taskScheduleQueryService.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskIdentifier)
    return ResponseEntity.ok()
        .eTag(schedule.version.toString())
        .body(taskScheduleResourceFactory.build(schedule))
  }

  @DeleteMapping(DAYCARDS_ENDPOINT)
  open fun deleteMultipleDayCardsAndSlotsFromSchedule(
      @RequestBody @Valid batchRequestResource: BatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") scheduleETag: ETag
  ): ResponseEntity<TaskScheduleResource> =
      if (identifierType == DAYCARD) {
        val taskIdentifier =
            dayCardQueryService.findTaskIdentifier(
                batchRequestResource.ids.asDayCardIds().stream().findAny().orElseThrow())!!
        val schedule =
            taskScheduleQueryService.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskIdentifier)

        val identifiers = batchRequestResource.ids.asDayCardIds().toList()

        deleteDayCardBatchCommandHandler.handle(
            listOf(DeleteDayCardsFromScheduleCommand(identifiers.toSet(), scheduleETag)),
            schedule.taskProjectIdentifier)

        val updatedSchedule =
            taskScheduleQueryService.findTaskScheduleWithDayCardsDtoByTaskIdentifier(taskIdentifier)

        ResponseEntity.ok()
            .eTag(updatedSchedule.version.toString())
            .body(taskScheduleResourceFactory.build(updatedSchedule))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @DeleteMapping(DAYCARDS_PROJECT_ENDPOINT)
  open fun deleteMultipleDayCardsAndSlots(
      @RequestBody @Valid deleteMultipleDayCardResource: DeleteMultipleDayCardResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String,
      @PathVariable projectId: ProjectId
  ): ResponseEntity<BatchResponseResource<TaskScheduleResource>> =
      if (identifierType == DAYCARD) {
        val identifiers = deleteMultipleDayCardResource.items.map { it.ids }.flatten().distinct()
        val taskIdentifiers =
            dayCardQueryService.findAllByIdentifierIn(identifiers.toSet()).map {
              it.taskScheduleTaskIdentifier
            }

        val deleteBatchCommand =
            deleteMultipleDayCardResource.items.map {
              DeleteDayCardsFromScheduleCommand(it.ids, ETag.from(it.scheduleVersion.toString()))
            }

        deleteDayCardBatchCommandHandler.handle(deleteBatchCommand, projectId)

        val schedules = taskScheduleQueryService.findByTaskIdentifiers(taskIdentifiers)
        ResponseEntity.ok().body(taskScheduleListResourceFactory.buildBatch(schedules))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @PostMapping(CANCEL_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun cancelDayCard(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardIdentifier: DayCardId,
      @RequestBody @Valid cancelDayCardResource: CancelDayCardResource,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") dayCardETag: ETag
  ): ResponseEntity<DayCardResource> {
    cancelDayCardCommandHandler.handle(
        CancelDayCardCommand(dayCardIdentifier, cancelDayCardResource.reason, dayCardETag))

    val dayCard = dayCardQueryService.findWithDetails(dayCardIdentifier)
    return ResponseEntity.ok()
        .eTag(dayCard.toEtagString())
        .body(dayCardResourceFactory.build(dayCard))
  }

  @PostMapping(CANCEL_DAYCARD_BATCH_ENDPOINT)
  open fun cancelMultipleDayCards(
      @RequestBody @Valid batchRequestResource: CancelMultipleDayCardsResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String
  ): ResponseEntity<BatchResponseResource<DayCardResource>> =
      if (identifierType == DAYCARD) {
        cancelDayCardBatchCommandHandler.handle(
            batchRequestResource.items, batchRequestResource.reason)

        val dayCards =
            dayCardQueryService.findAllByIdentifierIn(
                batchRequestResource.getIdentifiers().asDayCardIds())
        ResponseEntity.ok().body(dayCardBatchResourceFactory.build(dayCards))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @PostMapping(COMPLETE_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun completeDayCard(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardIdentifier: DayCardId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") dayCardETag: ETag
  ): ResponseEntity<DayCardResource> {
    completeDayCardCommandHandler.handle(CompleteDayCardCommand(dayCardIdentifier, dayCardETag))

    val dayCard = dayCardQueryService.findWithDetails(dayCardIdentifier)
    return ResponseEntity.ok()
        .eTag(dayCard.toEtagString())
        .body(dayCardResourceFactory.build(dayCard))
  }

  @PostMapping(COMPLETE_DAYCARDS_BATCH_ENDPOINT)
  open fun completeMultipleDayCards(
      @RequestBody @Valid batchRequestResource: VersionedUpdateBatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String
  ): ResponseEntity<BatchResponseResource<DayCardResource>> =
      if (identifierType == DAYCARD) {
        completeDayCardBatchCommandHandler.handle(batchRequestResource.items)

        val dayCards =
            dayCardQueryService.findAllByIdentifierIn(
                batchRequestResource.getIdentifiers().asDayCardIds())
        ResponseEntity.ok().body(dayCardBatchResourceFactory.build(dayCards))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @PostMapping(APPROVE_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun approveDayCard(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardIdentifier: DayCardId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") dayCardETag: ETag
  ): ResponseEntity<DayCardResource> {
    approveDayCardCommandHandler.handle(ApproveDayCardCommand(dayCardIdentifier, dayCardETag))

    val dayCard = dayCardQueryService.findWithDetails(dayCardIdentifier)
    return ResponseEntity.ok()
        .eTag(dayCard.toEtagString())
        .body(dayCardResourceFactory.build(dayCard))
  }

  @PostMapping(APPROVE_DAYCARDS_BATCH_ENDPOINT)
  open fun approveMultipleDayCards(
      @RequestBody @Valid batchRequestResource: VersionedUpdateBatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String
  ): ResponseEntity<BatchResponseResource<DayCardResource>> =
      if (identifierType == DAYCARD) {
        approveDayCardBatchCommandHandler.handle(batchRequestResource.items)

        val dayCards =
            dayCardQueryService.findAllByIdentifierIn(
                batchRequestResource.getIdentifiers().asDayCardIds())
        ResponseEntity.ok().body(dayCardBatchResourceFactory.build(dayCards))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  @PostMapping(RESET_DAYCARD_BY_DAYCARD_ID_ENDPOINT)
  open fun resetDayCard(
      @PathVariable(PATH_VARIABLE_DAY_CARD_ID) dayCardIdentifier: DayCardId,
      @Parameter(`in` = ParameterIn.HEADER, name = "If-Match") dayCardETag: ETag
  ): ResponseEntity<DayCardResource> {
    resetDayCardCommandHandler.handle(ResetDayCardCommand(dayCardIdentifier, dayCardETag))

    val dayCard = dayCardQueryService.findWithDetails(dayCardIdentifier)
    return ResponseEntity.ok()
        .eTag(dayCard.toEtagString())
        .body(dayCardResourceFactory.build(dayCard))
  }

  @PostMapping(RESET_DAYCARDS_BATCH_ENDPOINT)
  open fun resetMultipleDayCards(
      @RequestBody @Valid batchRequestResource: VersionedUpdateBatchRequestResource,
      @RequestParam(name = "identifierType", defaultValue = DAYCARD) identifierType: String
  ): ResponseEntity<BatchResponseResource<DayCardResource>> =
      if (identifierType == DAYCARD) {
        resetDayCardBatchCommandHandler.handle(batchRequestResource.items)

        val dayCards =
            dayCardQueryService.findAllByIdentifierIn(
                batchRequestResource.getIdentifiers().asDayCardIds())
        ResponseEntity.ok().body(dayCardBatchResourceFactory.build(dayCards))
      } else {
        throw BatchIdentifierTypeNotSupportedException(
            COMMON_VALIDATION_ERROR_IDENTIFIER_TYPE_NOT_SUPPORTED)
      }

  companion object {
    const val DAYCARD_BY_TASK_ID_ENDPOINT = "/projects/tasks/{taskId}/schedule/daycards"
    const val DAYCARD_BY_DAYCARD_ID_ENDPOINT = "/projects/tasks/schedule/daycards/{dayCardId}"
    const val DAYCARD_BY_TASK_ID_AND_DAYCARD_ID_ENDPOINT =
        "/projects/tasks/{taskId}/schedule/daycards/{dayCardId}"
    const val DAYCARDS_ENDPOINT = "/projects/tasks/schedule/daycards"
    const val CANCEL_DAYCARD_BY_DAYCARD_ID_ENDPOINT =
        "/projects/tasks/schedule/daycards/{dayCardId}/cancel"
    const val CANCEL_DAYCARD_BATCH_ENDPOINT = "/projects/tasks/schedule/daycards/cancel"
    const val COMPLETE_DAYCARD_BY_DAYCARD_ID_ENDPOINT =
        "/projects/tasks/schedule/daycards/{dayCardId}/complete"
    const val COMPLETE_DAYCARDS_BATCH_ENDPOINT = "/projects/tasks/schedule/daycards/complete"
    const val APPROVE_DAYCARD_BY_DAYCARD_ID_ENDPOINT =
        "/projects/tasks/schedule/daycards/{dayCardId}/approve"
    const val APPROVE_DAYCARDS_BATCH_ENDPOINT = "/projects/tasks/schedule/daycards/approve"
    const val RESET_DAYCARD_BY_DAYCARD_ID_ENDPOINT =
        "/projects/tasks/schedule/daycards/{dayCardId}/reset"
    const val RESET_DAYCARDS_BATCH_ENDPOINT = "/projects/tasks/schedule/daycards/reset"
    const val PATH_VARIABLE_TASK_ID = "taskId"
    const val PATH_VARIABLE_DAY_CARD_ID = "dayCardId"

    const val DAYCARDS_PROJECT_ENDPOINT = "/projects/{projectId}/tasks/schedule/daycards"
    const val PATH_VARIABLE_PROJECT_ID = "projectId"
  }
}
