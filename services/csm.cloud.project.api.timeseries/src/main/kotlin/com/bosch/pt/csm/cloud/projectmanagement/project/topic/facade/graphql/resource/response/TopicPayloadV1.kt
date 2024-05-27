/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.facade.graphql.resource.response

import java.time.LocalDateTime
import java.util.UUID

data class TopicPayloadV1(
    val id: UUID,
    val version: Long,
    val criticality: String,
    val description: String? = null,
    val eventDate: LocalDateTime
)
