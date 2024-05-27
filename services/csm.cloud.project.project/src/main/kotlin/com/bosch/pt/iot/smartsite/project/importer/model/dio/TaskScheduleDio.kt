/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_SCHEDULE_INVALID_DATES
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskScheduleIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.taskschedule.boundary.dto.SaveTaskScheduleBatchDto
import com.bosch.pt.iot.smartsite.project.taskschedule.domain.asTaskScheduleId
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

data class TaskScheduleDio(
    override val id: TaskScheduleIdentifier,
    override val guid: UUID?,
    override val uniqueId: Int,
    override val fileId: Int,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val taskId: TaskIdentifier
) : DataImportObject<SaveTaskScheduleBatchDto> {

  override val identifier: UUID = randomUUID()

  override val wbs: String? = null

  override val externalIdentifier: UUID
    get() = throw UnsupportedOperationException("Not implemented")

  override val objectType: ObjectType
    get() = throw UnsupportedOperationException("Not implemented")

  override val activityId: String
    get() = throw UnsupportedOperationException("Not implemented")

  override fun toTargetType(context: ImportContext): SaveTaskScheduleBatchDto =
      SaveTaskScheduleBatchDto(
          identifier.asTaskScheduleId(),
          null,
          requireNotNull(context[taskId]?.asTaskId()),
          start?.toLocalDate(),
          end?.toLocalDate(),
          null)

  override fun validate(context: ImportContext): List<ValidationResult> =
      mutableListOf<ValidationResult>().apply {
        if (start == null && end == null || start != null && end != null && start.isAfter(end)) {
          val taskDio = requireNotNull(context.dioMap[taskId]) as TaskDio
          this.add(
              ValidationResult(
                  ValidationResultType.ERROR,
                  taskDio.name,
                  IMPORT_VALIDATION_TASK_SCHEDULE_INVALID_DATES))
        }
      }
}
