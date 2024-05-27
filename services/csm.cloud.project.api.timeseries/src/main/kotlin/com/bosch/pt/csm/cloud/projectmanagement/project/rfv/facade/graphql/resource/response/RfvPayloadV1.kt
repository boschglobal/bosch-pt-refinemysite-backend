/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.graphql.resource.response

import java.time.LocalDateTime
import java.util.UUID

data class RfvPayloadV1(
    val id: UUID,
    val version: Long,
    val reason: String,
    val name: String,
    val active: Boolean,
    val eventDate: LocalDateTime
)
