/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractResource
import org.springframework.hateoas.Link
import org.springframework.hateoas.RepresentationModel

class MessageListResource : RepresentationModel<AbstractResource> {

  val messages: List<MessageResource>

  constructor(messages: List<MessageResource>) {
    this.messages = messages
  }

  constructor(initialLink: Link, messages: List<MessageResource>) : super(initialLink) {
    this.messages = messages
  }

  constructor(initialLinks: Iterable<Link>, messages: List<MessageResource>) : super(initialLinks) {
    this.messages = messages
  }

  companion object {
    const val LINK_PREVIOUS = "prev"
  }
}
