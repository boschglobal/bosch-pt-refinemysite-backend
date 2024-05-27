/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_ACTIVITY_ID_TOO_LONG
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NAME_EMPTY_DEFAULT_SET
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NOTES_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_MILESTONE_NO_DATE
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_WBS_NAME_TOO_LONG
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId.Companion.MAX_ACTIVITY_ID_LENGTH
import com.bosch.pt.iot.smartsite.project.external.model.ExternalId.Companion.MAX_WBS_LENGTH
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.CraftIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.MilestoneIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.WorkAreaIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.ERROR
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType.INFO
import com.bosch.pt.iot.smartsite.project.milestone.command.api.CreateMilestoneCommand
import com.bosch.pt.iot.smartsite.project.milestone.domain.asMilestoneId
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone.Companion.MAX_DESCRIPTION_LENGTH
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.Milestone.Companion.MAX_NAME_LENGTH
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

data class MilestoneDio(
    override val id: MilestoneIdentifier,
    override val guid: UUID?,
    override val uniqueId: Int,
    override val fileId: Int,
    override val activityId: String?,
    val name: String?,
    val type: MilestoneTypeEnum,
    val date: LocalDate?,
    val header: Boolean,
    val projectId: ProjectIdentifier,
    val notes: String?,
    val craftId: CraftIdentifier?,
    val workAreaId: WorkAreaIdentifier?
) : DataImportObject<CreateMilestoneCommand> {

  override val identifier: UUID = randomUUID()

  override val externalIdentifier: UUID = randomUUID()

  override val objectType: ObjectType = ObjectType.MILESTONE

  override val wbs: String? = null

  private val nameToImport: String by lazy { convertNameForImport() }

  override fun toTargetType(context: ImportContext): CreateMilestoneCommand =
      CreateMilestoneCommand(
          identifier.asMilestoneId(),
          requireNotNull(context[projectId]?.asProjectId()),
          nameToImport,
          type,
          requireNotNull(date),
          header,
          notes?.trim()?.take(MAX_DESCRIPTION_LENGTH),
          context[craftId]?.asProjectCraftId(),
          context[workAreaId]?.asWorkAreaId())

  override fun validate(context: ImportContext): List<ValidationResult> =
      mutableListOf<ValidationResult>().apply {
        if (name.isNullOrBlank())
            this.add(
                ValidationResult(
                    INFO, nameToImport, IMPORT_VALIDATION_MILESTONE_NAME_EMPTY_DEFAULT_SET))
        else if (name.length > MAX_NAME_LENGTH)
            this.add(
                ValidationResult(
                    INFO,
                    name,
                    IMPORT_VALIDATION_MILESTONE_NAME_WILL_BE_SHORTENED,
                ))

        if (date == null) {
          this.add(
              ValidationResult(
                  ERROR,
                  nameToImport,
                  IMPORT_VALIDATION_MILESTONE_NO_DATE,
              ))
        }

        if (notes != null && notes.length > MAX_DESCRIPTION_LENGTH) {
          this.add(
              ValidationResult(INFO, notes, IMPORT_VALIDATION_MILESTONE_NOTES_WILL_BE_SHORTENED))
        }

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
      if (name.isNullOrBlank()) PLACEHOLDER_MILESTONE_NAME else name.take(MAX_NAME_LENGTH)

  companion object {
    const val PLACEHOLDER_MILESTONE_NAME = "Unnamed Milestone"
  }
}
