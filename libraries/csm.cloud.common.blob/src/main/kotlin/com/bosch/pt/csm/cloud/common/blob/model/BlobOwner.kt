/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.blob.model

import com.bosch.pt.csm.cloud.common.blob.model.BlobMetadata.OwnerType
import java.util.UUID

/**
 * An entity that owns a [Blob]. A Blob knows its owner by its identifier (compare [ ]). Information
 * provided by this interface helps in building hierarchical Blob names.
 */
interface BlobOwner {

  /** the identifier of this Blob owner as a UUID */
  fun getIdentifierUuid(): UUID

  fun getOwnerType(): OwnerType

  /** the bounded context of the Blob owner to be reflected in the hierarchical Blob name */
  fun getBoundedContext(): BoundedContext

  /** this identifier will form the direct parent folder of the owned Blob */
  fun getParentIdentifier(): UUID
}
