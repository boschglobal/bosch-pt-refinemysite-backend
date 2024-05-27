/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.rfv.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.rfv.domain.RfvId

data class RfvResource(
    val id: RfvId,
    val version: Long,
    val project: ProjectId,
    val reason: String,
    val active: Boolean,
    val language: String,
    val name: String,
    val deleted: Boolean = false,
    val eventTimestamp: Long
)
