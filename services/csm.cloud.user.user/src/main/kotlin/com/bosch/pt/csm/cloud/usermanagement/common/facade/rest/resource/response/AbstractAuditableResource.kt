/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import java.util.UUID

/** Common base class for all resources that represent an auditable entity. */
abstract class AbstractAuditableResource : AbstractResource {

  open val createdDate: Date?
  val lastModifiedDate: Date?
  val createdBy: ResourceReference?
  val lastModifiedBy: ResourceReference?

  @get:JsonProperty("id") val identifier: UUID?
  val version: Long?

  /** Default constructor used by Jackson. */
  constructor() {
    createdDate = null
    lastModifiedDate = null
    createdBy = null
    lastModifiedBy = null
    identifier = null
    version = null
  }

  constructor(
      entity: AbstractSnapshotEntity<Long, *>,
      createdByName: String,
      lastModifiedByName: String
  ) {
    createdDate = entity.createdDate.map { it.toDate() }.orElse(null)
    lastModifiedDate = entity.lastModifiedDate.map { it.toDate() }.orElse(null)
    createdBy = ResourceReference(entity.createdBy.get().identifier, createdByName)
    lastModifiedBy = ResourceReference(entity.createdBy.get().identifier, lastModifiedByName)
    identifier = entity.getIdentifierUuid()
    version = entity.version
  }
}
