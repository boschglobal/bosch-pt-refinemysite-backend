/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.relation.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationController
import com.bosch.pt.iot.smartsite.project.relation.facade.rest.RelationSearchController
import com.bosch.pt.iot.smartsite.project.relation.model.Relation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
open class RelationListResourceFactory(
    private val factoryHelper: RelationResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  open fun build(relations: Page<Relation>, pageable: Pageable, projectId: ProjectId) =
      ListResponseResource(
              items = factoryHelper.build(relations.content),
              pageNumber = relations.number,
              pageSize = relations.size,
              totalPages = relations.totalPages,
              totalElements = relations.totalElements)
          .apply {
            addIf(relations.hasPrevious()) { prevLink(pageable, projectId) }
            addIf(relations.hasNext()) { nextLink(pageable, projectId) }
          }

  private fun prevLink(pageable: Pageable, projectId: ProjectId) =
      linkFactory
          .linkTo(RelationSearchController.RELATION_SEARCH_ENDPOINT)
          .withParameters(mapOf(RelationController.PATH_VARIABLE_PROJECT_ID to projectId))
          .withQueryParameters(mapOf("pageable" to pageable.previousOrFirst()))
          .withRel(LINK_PREVIOUS)

  private fun nextLink(pageable: Pageable, projectId: ProjectId) =
      linkFactory
          .linkTo(RelationSearchController.RELATION_SEARCH_ENDPOINT)
          .withParameters(mapOf(RelationController.PATH_VARIABLE_PROJECT_ID to projectId))
          .withQueryParameters(mapOf("pageable" to pageable.next()))
          .withRel(LINK_NEXT)

  companion object {
    const val LINK_PREVIOUS = "prev"
    const val LINK_NEXT = "next"
    const val PARAM_PAGE = "page"
    const val PARAM_SIZE = "size"
  }
}
