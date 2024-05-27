/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.graphql.resource.response

import java.time.LocalDateTime
import java.util.UUID

data class ProjectCraftPayloadV1(
    val id: UUID,
    val version: Long,
    val name: String,
    val color: String,
    val eventDate: LocalDateTime
)
