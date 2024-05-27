/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import org.springframework.hateoas.RepresentationModel

class TopicListResource(val topics: List<TopicResource>) : RepresentationModel<AbstractResource>() {

  companion object {
    const val LINK_PREVIOUS = "prev"
  }
}
