/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.project.facade.graphql.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.query.model.ProjectAddress
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ProjectPayloadV1(
    val id: UUID,
    val version: Long,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val projectNumber: String,
    val client: String? = null,
    val description: String? = null,
    val category: String? = null,
    val projectAddress: ProjectAddress,
    val eventDate: LocalDateTime,
)
