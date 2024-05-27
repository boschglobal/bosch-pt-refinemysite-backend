/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary

import com.bosch.pt.iot.smartsite.project.importer.boundary.resolver.ImportedEntityResolver
import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObjectIdentifier
import java.util.UUID

class ImportContext(
    val map: MutableMap<DataImportObjectIdentifier, UUID>,
    val dioMap: MutableMap<DataImportObjectIdentifier, Any>,
    private vararg val resolvers: ImportedEntityResolver<DataImportObjectIdentifier>
) {

  operator fun get(id: DataImportObjectIdentifier?): UUID? = id?.let { map[it] }

  fun get(
      id: Int,
      vararg expectedTypes: Class<out DataImportObjectIdentifier>
  ): DataImportObjectIdentifier? =
      map.keys.firstOrNull { it.id == id && expectedTypes.contains(it.javaClass) }

  @Suppress("UNCHECKED_CAST") fun <T> getDio(id: DataImportObjectIdentifier): T = dioMap[id] as T

  fun <T> lookupEntityFor(id: DataImportObjectIdentifier): T? =
      resolvers.firstOrNull { it.handles(id.javaClass) }.let { it?.resolve<T>(id) }
}
