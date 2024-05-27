/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.quickfilter.facade.rest.resources.response

import org.springframework.hateoas.RepresentationModel

data class QuickFilterListResource(val items: Collection<QuickFilterResource>) :
    RepresentationModel<QuickFilterListResource>() {

  companion object {
    const val LINK_FILTER_CREATE = "create"
  }
}
