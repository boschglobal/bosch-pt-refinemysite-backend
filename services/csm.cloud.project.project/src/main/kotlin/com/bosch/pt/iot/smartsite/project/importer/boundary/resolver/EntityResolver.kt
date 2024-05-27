/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.resolver

import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObjectIdentifier
import com.bosch.pt.iot.smartsite.project.importer.model.identifier.ProjectIdentifier

class EntityResolver(val objects: Map<DataImportObjectIdentifier, Any>) :
    ImportedEntityResolver<DataImportObjectIdentifier> {

  override fun handles(expectedType: Class<*>): Boolean =
      expectedType == ProjectIdentifier::class.java

  @Suppress("UNCHECKED_CAST")
  override fun <T> resolve(id: DataImportObjectIdentifier): T = requireNotNull(objects[id]) as T
}
