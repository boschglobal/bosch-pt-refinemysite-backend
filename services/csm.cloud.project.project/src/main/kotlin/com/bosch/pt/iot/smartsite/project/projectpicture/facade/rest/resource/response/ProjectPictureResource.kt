/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.util.Date
import java.util.UUID

data class ProjectPictureResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val projectReference: ResourceReference,
    val width: Long = 400L,
    val height: Long = 400L,
    val fileSize: Long = 19664L
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_FULL = "full"
    const val LINK_SMALL = "small"
    const val LINK_DELETE = "delete"
  }
}
