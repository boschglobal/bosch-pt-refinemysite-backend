/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.response.AbstractAuditableResource
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import java.util.function.Supplier

class RelationResource(
    val type: RelationTypeEnum,
    val source: RelationElementDto,
    val target: RelationElementDto,
    val critical: Boolean?,
    relation: Relation,
    deletedUserReference: Supplier<ResourceReference>,
) : AbstractAuditableResource(relation, deletedUserReference) {

  companion object {
    const val LINK_DELETE = "delete"
  }
}
