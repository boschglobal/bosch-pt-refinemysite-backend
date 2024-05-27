/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.model.RelationTypeEnum
import jakarta.validation.constraints.Size

data class FilterRelationResource(
    val types: Collection<RelationTypeEnum> = emptySet(),
    // we keep a limit of 10,000 to reduce the impact of a potential DoS attack; at the same time,
    // the limit is big enough so that clients don't need to care about the limit in realistic
    // scenarios.
    @field:Size(max = 10_000) val sources: Collection<RelationElementDto> = emptySet(),
    @field:Size(max = 10_000) val targets: Collection<RelationElementDto> = emptySet()
)
