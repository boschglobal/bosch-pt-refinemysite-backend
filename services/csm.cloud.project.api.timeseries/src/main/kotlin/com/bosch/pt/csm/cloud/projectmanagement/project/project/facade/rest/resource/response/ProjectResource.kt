/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectAddress
import java.time.LocalDate

data class ProjectResource(
    val id: ProjectId,
    val version: Long,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val client: String? = null,
    val description: String? = null,
    val category: String? = null,
    val projectAddress: ProjectAddress,
    val eventTimestamp: Long
)
