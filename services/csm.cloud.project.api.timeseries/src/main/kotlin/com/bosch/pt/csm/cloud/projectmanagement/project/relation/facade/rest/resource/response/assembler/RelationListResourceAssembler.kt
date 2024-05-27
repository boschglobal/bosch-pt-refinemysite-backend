/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.RelationListResource
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.Relation
import org.springframework.stereotype.Component

@Component
class RelationListResourceAssembler(
    private val relationResourceAssembler: RelationResourceAssembler
) {

  fun assemble(relations: List<Relation>, latestOnly: Boolean): RelationListResource =
      RelationListResource(
          relations
              .flatMap { relationResourceAssembler.assemble(it, latestOnly) }
              .sortedWith(compareBy({ it.id.value }, { it.version }, { it.eventTimestamp })))
}
