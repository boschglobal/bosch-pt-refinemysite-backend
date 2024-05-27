/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum

data class CreateRelationResource(
    val type: RelationTypeEnum,
    val source: RelationElementDto,
    val target: RelationElementDto
) {

  fun toRelationDto() = RelationDto(type, source, target)
}
