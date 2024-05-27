/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarealist.facade.graphql.resource.response

import java.time.LocalDateTime
import java.util.UUID

data class WorkAreaListPayloadV1(
    val id: UUID,
    val version: Long,
    val items: List<UUID>,
    val eventDate: LocalDateTime
)
