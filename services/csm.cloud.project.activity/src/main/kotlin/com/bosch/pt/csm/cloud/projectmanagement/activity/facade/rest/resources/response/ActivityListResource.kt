/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response

import org.springframework.hateoas.RepresentationModel

data class ActivityListResource(val activities: Collection<ActivityResource>) :
    RepresentationModel<ActivityListResource>() {

  companion object {
    const val LINK_PREVIOUS = "prev"
  }
}
