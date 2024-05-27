/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.util.Date
import java.util.UUID

data class ProjectCraftResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val project: ResourceReference,
    val name: String,
    val color: String
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_UPDATE = "update"
    const val LINK_DELETE = "delete"
  }
}
