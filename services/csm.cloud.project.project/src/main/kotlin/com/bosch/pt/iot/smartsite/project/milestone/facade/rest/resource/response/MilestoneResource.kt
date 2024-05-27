/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.milestone.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.project.milestone.shared.model.MilestoneTypeEnum
import java.time.LocalDate
import java.util.Date
import java.util.UUID

data class MilestoneResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val name: String,
    val type: MilestoneTypeEnum,
    val date: LocalDate,
    val header: Boolean,
    val project: ResourceReference,
    val description: String?,
    val craft: MilestoneProjectCraftReference?,
    val workArea: ResourceReference?,
    val creator: ResourceReferenceWithPicture,
    val position: Int,
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_MILESTONE_UPDATE = "update"
    const val LINK_MILESTONE_DELETE = "delete"
    const val LINK_CREATE_CRAFT_MILESTONE = "createCraftMilestone"
    const val LINK_CREATE_INVESTOR_MILESTONE = "createInvestorMilestone"
    const val LINK_CREATE_PROJECT_MILESTONE = "createProjectMilestone"
  }
}
