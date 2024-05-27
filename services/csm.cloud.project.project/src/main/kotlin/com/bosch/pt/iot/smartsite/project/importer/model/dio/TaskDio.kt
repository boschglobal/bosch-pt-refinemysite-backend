/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_ACTIVITY_ID_TOO_LONG
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_CRAFT_ADDITIONAL_VALUES_NOT_CONSIDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_NAME_EMPTY_DEFAULT_SET
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_NOTES_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_TASK_WORKING_AREA_ADDITIONAL_VALUES_NOT_CONSIDERED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WBS_NAME_TOO_LONG
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId.Companion.MAX_WBS_LENGTH
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.CraftIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.TaskIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.ERROR
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.domain.asTaskId
import com.bosch.pt.iot.smartsite.project.task.shared.dto.SaveTaskBatchDto
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task.Companion.MAX_DESCRIPTION_LENGTH
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.util.UUID
import java.util.UUID.randomUUID

data class TaskDio(
    override val id: TaskIdentifier,
    override val guid: UUID?,
    override val uniqueId: Int,
    override val fileId: Int,
    override val activityId: String?,
    val name: String?,
    val notes: String?,
    val craftId: CraftIdentifier,
    val workAreaId: WorkAreaIdentifier?,
    val status: TaskStatusEnum,
    // If crafts are modelled as resources there can be multiple values assigned.
    // We just keep the first name and drop the others.
    // We create a warning though and need all names for it.
    val craftNamesFromResources: List<String>,
    // If work areas are modelled as resources there can be multiple values assigned.
    // We just keep the first name and drop the others.
    // We create a warning though and need all names for it.
    val workAreaNamesFromResources: List<String>
) : DataImportObject<SaveTaskBatchDto> {

  override val identifier: UUID = randomUUID()

  override val externalIdentifier: UUID = randomUUID()

  override val objectType: ObjectType = ObjectType.TASK

  override val wbs: String? = null

  private val nameToImport: String by lazy { convertNameForImport() }

  override fun toTargetType(context: ImportContext) =
      SaveTaskBatchDto(
          identifier.asTaskId(),
          null,
          nameToImport,
          notes?.trim()?.take(MAX_DESCRIPTION_LENGTH),
          null,
          status,
          requireNotNull(context[craftId]).asProjectCraftId(),
          null,
          context[workAreaId]?.asWorkAreaId())

  override fun validate(context: ImportContext): List<ValidationResult> =
      mutableListOf<ValidationResult>().apply {
        if (name.isNullOrBlank())
            this.add(
                ValidationResult(
                    ValidationResultType.INFO,
                    nameToImport,
                    IMPORT_VALIDATION_TASK_NAME_EMPTY_DEFAULT_SET))
        else if (name.length > MAX_NAME_LENGTH)
            this.add(
                ValidationResult(
                    ValidationResultType.INFO, name, IMPORT_VALIDATION_TASK_NAME_WILL_BE_SHORTENED))

        if (notes != null && notes.length > MAX_DESCRIPTION_LENGTH) {
          this.add(
              ValidationResult(
                  ValidationResultType.INFO, notes, IMPORT_VALIDATION_TASK_NOTES_WILL_BE_SHORTENED))
        }

        if (craftNamesFromResources.size > 1) {
          this.add(
              ValidationResult(
                  ValidationResultType.INFO,
                  craftNamesFromResources.joinToString(separator = ", "),
                  IMPORT_VALIDATION_TASK_CRAFT_ADDITIONAL_VALUES_NOT_CONSIDERED))
        }

        if (workAreaNamesFromResources.size > 1) {
          this.add(
              ValidationResult(
                  ValidationResultType.INFO,
                  workAreaNamesFromResources.joinToString(separator = ", "),
                  IMPORT_VALIDATION_TASK_WORKING_AREA_ADDITIONAL_VALUES_NOT_CONSIDERED))
        }

        if (wbs != null && wbs.length > MAX_WBS_LENGTH) {
          this.add(
              ValidationResult(
                  ERROR, "...${wbs.takeLast(100)}", IMPORT_VALIDATION_WBS_NAME_TOO_LONG))
        }

        if (activityId != null && activityId.length > ExternalId.MAX_ACTIVITY_ID_LENGTH) {
          this.add(
              ValidationResult(
                  ERROR, "...${activityId.takeLast(100)}", IMPORT_VALIDATION_ACTIVITY_ID_TOO_LONG))
        }
      }

  private fun convertNameForImport(): String =
      if (name.isNullOrBlank()) PLACEHOLDER_TASK_NAME else name.trim().take(MAX_NAME_LENGTH)

  companion object {
    const val PLACEHOLDER_TASK_NAME = "Unnamed Task"
  }
}
