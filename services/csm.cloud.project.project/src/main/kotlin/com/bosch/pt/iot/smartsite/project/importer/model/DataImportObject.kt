/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.model

import com.bosch.pt.iot.smartsite.project.external.model.ExternalId
import com.bosch.pt.iot.smartsite.project.external.model.ExternalIdType
import com.bosch.pt.iot.smartsite.project.external.model.ObjectType
import com.bosch.pt.iot.smartsite.project.importer.boundary.ImportContext
import com.bosch.pt.iot.smartsite.project.importer.validation.ValidationResult
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import java.util.UUID

interface DataImportObject<T> {

  val id: DataImportObjectIdentifier

  val identifier: UUID

  val objectType: ObjectType

  val guid: UUID?

  val uniqueId: Int

  val fileId: Int

  val activityId: String?

  val wbs: String?

  val externalIdentifier: UUID

  fun toTargetType(context: ImportContext): T

  fun validate(context: ImportContext): List<ValidationResult>

  fun toExternalId(projectId: ProjectId, type: ExternalIdType) =
      ExternalId(
          identifier = externalIdentifier,
          projectId = projectId,
          idType = type,
          objectIdentifier = identifier,
          objectType = objectType,
          guid = guid ?: identifier,
          uniqueId = uniqueId,
          fileId = fileId,
          activityId = activityId,
          wbs = wbs)
}
