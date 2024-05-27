/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response

import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.AuditableResource
import com.bosch.pt.iot.smartsite.dataimport.common.api.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectAddress
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectCategoryEnum
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectConstructionSiteManager
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.Date
import java.util.UUID

@JsonIgnoreProperties("_embedded", "_links")
class ProjectResource(
    val id: UUID,
    val identifier: UUID? = null,
    val constructionSiteManagerId: String? = null,
    val client: String? = null,
    val description: String? = null,
    val end: Date? = null,
    val start: Date? = null,
    val projectNumber: String? = null,
    val title: String? = null,
    val category: ProjectCategoryEnum? = null,
    val address: ProjectAddress? = null,
    val participants: Int? = null,
    val constructionSiteManager: ProjectConstructionSiteManager? = null,
    val company: ResourceReference? = null
) : AuditableResource()
