/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.project.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectCategoryEnum
import java.time.LocalDate
import java.util.Date
import java.util.UUID

data class ProjectResource(
    override val id: UUID,
    override val version: Long,
    override val createdDate: Date,
    override val createdBy: ResourceReference,
    override val lastModifiedDate: Date,
    override val lastModifiedBy: ResourceReference,
    val client: String?,
    val description: String?,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val title: String,
    val category: ProjectCategoryEnum?,
    val address: ProjectAddressDto?,
    val participants: Long,
    val company: ResourceReference?,
    val constructionSiteManager: ProjectConstructionSiteManagerDto?,
) :
    AbstractAuditableResource(
        id, version, createdDate, createdBy, lastModifiedDate, lastModifiedBy) {

  companion object {
    const val LINK_PROJECT_WORKAREAS = "workAreas"
    const val LINK_PROJECT_CRAFTS = "projectCrafts"
    const val LINK_WORKDAY_CONFIGURATION = "workdays"
    const val LINK_PARTICIPANTS = "participants"
    const val LINK_COMPANIES = "companies"
    const val LINK_ASSIGN = "assign"
    const val LINK_TASKS = "tasks"
    const val LINK_MILESTONES = "milestones"
    const val LINK_RESCHEDULE = "reschedule"
    const val LINK_CREATE_PROJECT = "create"
    const val LINK_UPDATE_PROJECT = "update"
    const val LINK_DELETE_PROJECT = "delete"
    const val EMBEDDED_PROJECT_PICTURES = "projectPicture"
    const val EMBEDDED_PROJECT_STATISTICS = "statistics"
  }
}
