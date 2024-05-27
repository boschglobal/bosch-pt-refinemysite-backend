/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.domain.RelationId
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.RelationReference

data class RelationResource(
    val id: RelationId,
    val version: Long,
    val project: ProjectId,
    val critical: Boolean,
    val type: String,
    val source: RelationReference,
    val target: RelationReference,
    val deleted: Boolean = false,
    val eventTimestamp: Long
)
