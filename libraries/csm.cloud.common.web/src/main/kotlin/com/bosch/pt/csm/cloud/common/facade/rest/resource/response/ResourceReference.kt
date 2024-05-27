/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.Referable
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

open class ResourceReference

/**
 * A reference to another resource which consists of a technical UUID and a display name.
 *
 * @param identifier the UUID of the resource
 * @param displayName the displayName of the resource
 */
constructor(@get:JsonProperty("id") val identifier: UUID, val displayName: String) {

  companion object {

    /**
     * Returns a [ResourceReference] to the given [Referable].
     *
     * @param referable the [Referable] object.
     * @return a [ResourceReference]
     */
    fun from(referable: Referable) =
        ResourceReference(referable.getIdentifierUuid(), referable.getDisplayName()!!)
  }
}
