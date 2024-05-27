/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.assembler

import com.bosch.pt.csm.cloud.projectmanagement.project.relation.facade.rest.resource.response.RelationResource
import com.bosch.pt.csm.cloud.projectmanagement.project.relation.query.model.Relation
import org.springframework.stereotype.Component

@Component
class RelationResourceAssembler {

  fun assemble(relation: Relation, latestOnly: Boolean): List<RelationResource> =
      if (latestOnly) {
        listOf(
            RelationResourceMapper.INSTANCE.fromRelationVersion(
                relation.history.last(), relation.project, relation.identifier))
      } else {
        relation.history.map { relationVersion ->
          RelationResourceMapper.INSTANCE.fromRelationVersion(
              relationVersion, relation.project, relation.identifier)
        }
      }
}
