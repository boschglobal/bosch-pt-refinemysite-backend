/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.csm.cloud.common.ExcludeFromCodeCoverage
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_NAME_EMPTY_DEFAULT_SET
import com.bosch.pt.iot.smartsite.common.i18n.Key.IMPORT_VALIDATION_CRAFT_NAME_WILL_BE_SHORTENED
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.CraftIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResultType
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.command.api.CreateProjectCraftCommand
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft.Companion.MAX_NAME_LENGTH
import java.util.UUID
import java.util.UUID.randomUUID
import java.util.concurrent.atomic.AtomicInteger

data class CraftDio(
    override val id: CraftIdentifier,
    override val guid: UUID?,
    override val uniqueId: Int,
    override val fileId: Int,
    val name: String?,
    val color: String,
    val projectId: ProjectIdentifier,
    val placeholderCraftCount: AtomicInteger
) : DataImportObject<CreateProjectCraftCommand> {

  override val identifier: UUID = randomUUID()

  override val wbs: String? = null

  override val objectType: ObjectType
    get() = throw UnsupportedOperationException("Not implemented")

  override val externalIdentifier: UUID
    get() = throw UnsupportedOperationException("Not implemented")

  override val activityId: String
    get() = throw UnsupportedOperationException("Not implemented")

  val lookUpName: String?
    get() = name?.uppercase()

  private val nameToImport: String by lazy { convertNameForImport() }

  // The project import service now includes a version that starts at 0 and is incremented for
  // each added project craft. This addition ensures clean handlers and eliminates the need to
  // handle ProjectCraftList without a version in the command handlers.
  override fun toTargetType(context: ImportContext): CreateProjectCraftCommand =
      CreateProjectCraftCommand(
          projectIdentifier = requireNotNull(context[projectId]?.asProjectId()),
          identifier = identifier.asProjectCraftId(),
          name = nameToImport,
          color = color,
          projectCraftListVersion = 0,
          position = null)

  override fun validate(context: ImportContext): List<ValidationResult> =
      mutableListOf<ValidationResult>().apply {
        if (name.isNullOrBlank())
            this.add(
                ValidationResult(
                    ValidationResultType.INFO,
                    nameToImport,
                    IMPORT_VALIDATION_CRAFT_NAME_EMPTY_DEFAULT_SET))
        else if (name.length > MAX_NAME_LENGTH)
            this.add(
                ValidationResult(
                    ValidationResultType.INFO,
                    name,
                    IMPORT_VALIDATION_CRAFT_NAME_WILL_BE_SHORTENED))
      }

  private fun convertNameForImport(): String =
      if (name.isNullOrBlank()) PLACEHOLDER_CRAFT_NAME + placeholderCraftCount.getAndIncrement()
      else name.take(MAX_NAME_LENGTH)

  @ExcludeFromCodeCoverage
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CraftDio

    if (lookUpName != other.lookUpName) return false
    if (projectId != other.projectId) return false

    return true
  }

  @ExcludeFromCodeCoverage
  override fun hashCode(): Int {
    var result = lookUpName?.hashCode() ?: 0
    result = 31 * result + projectId.hashCode()
    return result
  }

  companion object {
    const val PLACEHOLDER_CRAFT_NAME = "RmS-Placeholder"
  }
}
