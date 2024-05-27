/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.facade.rest.resource.request

import java.util.UUID

data class VersionedIdentifier(override val id: UUID, val version: Long) : IdentifiableResource {

  companion object {

    @JvmStatic
    fun mapToSetOfIds(versionedIdentifiers: Collection<VersionedIdentifier>) =
        versionedIdentifiers.map(VersionedIdentifier::id).toSet()
  }
}
