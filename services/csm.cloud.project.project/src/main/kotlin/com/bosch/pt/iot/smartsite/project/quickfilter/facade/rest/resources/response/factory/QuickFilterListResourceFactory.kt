/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController.Companion.PATH_VARIABLE_PROJECT_ID
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.QuickFilterController.Companion.QUICK_FILTERS_ENDPOINT
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterListResource
import com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response.QuickFilterListResource.Companion.LINK_FILTER_CREATE
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter
import com.bosch.pt.iot.smartsite.project.quickfilter.model.QuickFilter.Companion.MAX_QUICK_FILTERS_PER_PARTICIPANT_IN_PROJECT
import org.springframework.hateoas.Link
import org.springframework.stereotype.Component

@Component
open class QuickFilterListResourceFactory(
    private val quickFilterResourceFactoryHelper: QuickFilterResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {
  open fun build(quickFilters: List<QuickFilter>, projectRef: ProjectId): QuickFilterListResource =
      QuickFilterListResource(quickFilterResourceFactoryHelper.build(quickFilters, projectRef))
          .apply {
            addIf(items.size < MAX_QUICK_FILTERS_PER_PARTICIPANT_IN_PROJECT) {
              createLink(projectRef)
            }
          }

  private fun createLink(projectRef: ProjectId): Link =
      linkFactory
          .linkTo(QUICK_FILTERS_ENDPOINT)
          .withParameters(mapOf(PATH_VARIABLE_PROJECT_ID to projectRef))
          .withRel(LINK_FILTER_CREATE)
}
