/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.Referable
import java.net.URI
import java.util.UUID

class ResourceReferenceWithPicture

/**
 * Creates a new reference with picture.
 *
 * @param identifier the UUID of the resource
 * @param displayName the displayName of the resource
 * @param picture the picture to represent the referenced resource
 */
constructor(identifier: UUID, displayName: String, var picture: URI) :
    ResourceReference(identifier, displayName) {

  companion object {

    /**
     * Returns a [ResourceReference] to the given [Referable].
     *
     * @param referable the [Referable] object.
     * @param picture the picture to represent the referenced resource
     * @return a [ResourceReference]
     */
    fun from(referable: Referable, picture: URI) =
        ResourceReferenceWithPicture(
            referable.getIdentifierUuid(), referable.getDisplayName()!!, picture)
  }
}
