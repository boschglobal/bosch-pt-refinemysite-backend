/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.facade.rest.resource.request

import com.bosch.pt.iot.smartsite.project.message.shared.model.Message.Companion.MAX_CONTENT_LENGTH
import jakarta.validation.constraints.Size

data class CreateMessageResource(@field:Size(max = MAX_CONTENT_LENGTH) val content: String?)
