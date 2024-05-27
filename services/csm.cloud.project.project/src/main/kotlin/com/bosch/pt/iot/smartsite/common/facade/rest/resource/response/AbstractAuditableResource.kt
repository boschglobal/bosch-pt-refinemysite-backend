/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import com.bosch.pt.iot.smartsite.common.model.ResourceReferenceAssembler.referTo
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import java.util.UUID
import java.util.function.Supplier

@LibraryCandidate("Move to common.web once the secondary constructor is no longer used")
abstract class AbstractAuditableResource(
    @get:JsonProperty("id") open val identifier: UUID,
    open val version: Long,
    open val createdDate: Date,
    open val createdBy: ResourceReference,
    open val lastModifiedDate: Date,
    open val lastModifiedBy: ResourceReference
) : AbstractResource() {

  @Deprecated("remove then all aggregates have been migrated")
  constructor(
      entity: AbstractEntity<Long, *>,
      deletedUserReference: Supplier<ResourceReference>
  ) : this(
      identifier = entity.identifier!!,
      version = entity.version!!,
      createdDate = entity.createdDate.map { it.toDate() }.get(),
      lastModifiedDate = entity.lastModifiedDate.map { it.toDate() }.get(),
      createdBy = entity.createdBy.get().let { referTo(it, deletedUserReference, it.deleted) },
      lastModifiedBy =
          entity.lastModifiedBy.get().let { referTo(it, deletedUserReference, it.deleted) })

  constructor(
      entity: AbstractReplicatedEntity<Long>,
      deletedUserReference: Supplier<ResourceReference>
  ) : this(
      identifier = entity.identifier!!,
      version = entity.version!!,
      createdDate = entity.createdDate.map { it.toDate() }.get(),
      lastModifiedDate = entity.lastModifiedDate.map { it.toDate() }.get(),
      createdBy = entity.createdBy.get().let { referTo(it, deletedUserReference, it.deleted) },
      lastModifiedBy =
          entity.lastModifiedBy.get().let { referTo(it, deletedUserReference, it.deleted) })
}
