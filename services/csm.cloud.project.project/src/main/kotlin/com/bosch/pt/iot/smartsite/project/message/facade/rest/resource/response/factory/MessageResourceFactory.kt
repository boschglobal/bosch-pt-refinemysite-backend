/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.factory

import com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.response.MessageResource
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import org.springframework.stereotype.Component

@Component
class MessageResourceFactory(
    private val messageResourceFactoryHelper: MessageResourceFactoryHelper
) {

  fun build(message: MessageDto): MessageResource? =
      messageResourceFactoryHelper.build(listOf(message), true).firstOrNull()
}
