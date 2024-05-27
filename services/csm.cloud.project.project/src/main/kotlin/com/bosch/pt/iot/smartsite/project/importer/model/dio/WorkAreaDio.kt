/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_ACTIVITY_ID_TOO_LONG
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WBS_NAME_TOO_LONG
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORK_AREA_NAME_EMPTY_DEFAULT_SET
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WORK_AREA_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId.Companion.MAX_ACTIVITY_ID_LENGTH
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId.Companion.MAX_WBS_LENGTH
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.ERROR
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.command.api.CreateWorkAreaCommand
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea.Companion.MAX_WORKAREA_NAME_LENGTH
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.atomic.AtomicInteger

data class WorkAreaDio(
    override val id: WorkAreaIdentifier,
    override val guid: UUID?,
    override val uniqueId: Int,
    override val fileId: Int,
    override val activityId: String?,
    override val wbs: String?,
    val name: String?,
    val parent: UUID?,
    val projectId: ProjectIdentifier,
    val placeholderWorkAreaCount: AtomicInteger
) : DataImportObject<CreateWorkAreaCommand> {

  override val identifier: UUID = randomUUID()

  override val externalIdentifier: UUID = randomUUID()

  override val objectType: ObjectType = ObjectType.WORKAREA

  val lookUpName: String
    get() = "$parent:${name?.uppercase()}"

  private val nameToImport: String by lazy { convertNameForImport() }

  // The project import service now includes a version that starts at 0 and is incremented for
  // each added workArea. This addition ensures clean handlers and eliminates the need to handle
  // WorkAreaLists without a version in the command handlers.
  override fun toTargetType(context: ImportContext): CreateWorkAreaCommand =
      CreateWorkAreaCommand(
          identifier.asWorkAreaId(),
          requireNotNull(context[projectId]?.asProjectId()),
          nameToImport,
          null,
          workAreaListVersion = 0,
          parent?.asWorkAreaId())

  override fun validate(context: ImportContext): List<ValidationResult> =
      mutableListOf<ValidationResult>().apply {
        if (name.isNullOrBlank())
            this.add(
                ValidationResult(
                    INFO, nameToImport, IMPORT_VALIDATION_WORK_AREA_NAME_EMPTY_DEFAULT_SET))
        else if (name.length > MAX_WORKAREA_NAME_LENGTH)
            this.add(
                ValidationResult(INFO, name, IMPORT_VALIDATION_WORK_AREA_NAME_WILL_BE_SHORTENED))

        if (wbs != null && wbs.length > MAX_WBS_LENGTH) {
          this.add(
              ValidationResult(
                  ERROR, "...${wbs.takeLast(100)}", IMPORT_VALIDATION_WBS_NAME_TOO_LONG))
        }

        if (activityId != null && activityId.length > MAX_ACTIVITY_ID_LENGTH) {
          this.add(
              ValidationResult(
                  ERROR, "...${activityId.takeLast(100)}", IMPORT_VALIDATION_ACTIVITY_ID_TOO_LONG))
        }
      }

  private fun convertNameForImport(): String =
      if (name.isNullOrBlank())
          PLACEHOLDER_WORK_AREA_NAME + placeholderWorkAreaCount.getAndIncrement()
      else name.trim().take(MAX_WORKAREA_NAME_LENGTH)

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is WorkAreaDio) return false

    if (projectId != other.projectId) return false
    return lookUpName == other.lookUpName
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    var result = projectId.hashCode()
    result = 31 * result + lookUpName.hashCode()
    return result
  }

  companion object {
    const val PLACEHOLDER_WORK_AREA_NAME = "Placeholder"
  }
}
