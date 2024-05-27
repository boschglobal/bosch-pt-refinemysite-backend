/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.taskconstraint.facade.graphql.resource.response

import java.time.LocalDateTime
import java.util.UUID

data class TaskConstraintPayloadV1(
    val id: UUID,
    val version: Long,
    val key: String,
    val name: String? = null,
    val active: Boolean,
    val eventDate: LocalDateTime
)
