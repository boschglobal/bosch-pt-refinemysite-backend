/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.util.Date
import java.util.UUID

data class WorkAreaResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val name: String,
    val project: ResourceReference,
    val parent: WorkAreaId?
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_PROJECT = "project"
    const val LINK_CREATE = "create"
    const val LINK_UPDATE = "update"
    const val LINK_DELETE = "delete"
    const val LINK_REORDER = "reorder"
  }
}
