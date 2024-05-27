/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.graphql.resource.response

import java.time.LocalDateTime
import java.util.UUID

data class WorkAreaPayloadV1(
    val id: UUID,
    val version: Long,
    val name: String,
    val eventDate: LocalDateTime
)
