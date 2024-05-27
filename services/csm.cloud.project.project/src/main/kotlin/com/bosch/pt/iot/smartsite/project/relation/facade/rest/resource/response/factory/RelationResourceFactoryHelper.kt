/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.common.facade.rest.resource.factory.AbstractResourceFactoryHelper
import com.bosch.pt.iot.smartsite.project.relation.authorization.RelationAuthorizationComponent
import com.bosch.pt.iot.smartsite.project.relation.boundary.dto.RelationDto.RelationElementDto
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationController
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.RelationResource
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import com.bosch.pt.iot.smartsite.project.relation.model.RelationElement
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
open class RelationResourceFactoryHelper(
    private val relationAuthorizationComponent: RelationAuthorizationComponent,
    private val linkFactory: CustomLinkBuilderFactory,
    messageSource: MessageSource
) : AbstractResourceFactoryHelper(messageSource) {

  open fun build(relations: List<Relation>): List<RelationResource> {
    if (relations.isEmpty()) {
      return emptyList()
    }

    val projectId = relations.map { it.project.identifier }.distinct().single()
    val relationIds = relations.map { it.identifier!! }.toSet()

    val relationDeletePermissions =
        relationAuthorizationComponent.filterRelationsWithDeletePermission(relationIds, projectId)

    return relations.map { build(it, relationDeletePermissions.contains(it.identifier)) }
  }

  open fun build(relation: Relation, allowedToDelete: Boolean) =
      RelationResource(
              relation.type,
              relation.source.toDto(),
              relation.target.toDto(),
              relation.critical,
              relation,
              deletedUserReference)
          .apply {
            // delete relation link
            addIf(allowedToDelete) {
              linkFactory
                  .linkTo(RelationController.RELATION_BY_RELATION_ID_ENDPOINT)
                  .withParameters(
                      mapOf(
                          RelationController.PATH_VARIABLE_PROJECT_ID to
                              relation.project.identifier,
                          RelationController.PATH_VARIABLE_RELATION_ID to relation.identifier!!))
                  .withRel(RelationResource.LINK_DELETE)
            }
          }

  private fun RelationElement.toDto() = RelationElementDto(identifier, type)
}
