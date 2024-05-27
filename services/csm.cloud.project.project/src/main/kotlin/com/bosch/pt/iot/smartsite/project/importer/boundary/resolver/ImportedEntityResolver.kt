/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.importer.boundary.resolver

import com.bosch.pt.iot.smartsite.project.importer.model.DataImportObjectIdentifier

interface ImportedEntityResolver<U : DataImportObjectIdentifier> {

  fun handles(expectedType: Class<*>): Boolean

  fun <T> resolve(id: U): T?
}
