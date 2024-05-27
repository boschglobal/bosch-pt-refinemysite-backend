/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model.dio

import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObject
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import java.util.UUID

data class ProjectDio(override val id: ProjectIdentifier, override val identifier: UUID) :
    DataImportObject<Project> {

  override val guid = null
  override val uniqueId: Int = -1
  override val fileId: Int = -1
  override val activityId: String? = null
  override val wbs: String? = null

  override val externalIdentifier: UUID
    get() = throw UnsupportedOperationException("Not implemented")

  override fun toTargetType(context: ImportContext): Project {
    throw UnsupportedOperationException("Not implemented")
  }

  override fun validate(context: ImportContext): List<ValidationResult> {
    throw UnsupportedOperationException("Not implemented")
  }

  override val objectType: ObjectType
    get() = throw UnsupportedOperationException("Not implemented")
}
