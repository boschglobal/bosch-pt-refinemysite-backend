/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.iot.smartsite.project.quickfilter.domain.QuickFilterId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.request.SaveQuickFilterResource.CriteriaResource
import java.util.Date

class QuickFilterResource(
    identifier: QuickFilterId,
    version: Long,
    val name: String,
    val useMilestoneCriteria: Boolean,
    val useTaskCriteria: Boolean,
    val highlight: Boolean,
    val criteria: CriteriaResource,
    createdDate: Date,
    lastModifiedDate: Date,
    createdBy: ResourceReference,
    lastModifiedBy: ResourceReference
) :
    AbstractAuditableResource(
        identifier.identifier, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_FILTER_UPDATE = "update"
    const val LINK_FILTER_DELETE = "delete"
  }
}
