/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request

import java.util.UUID

class CreateMessageResource(val content: String? = null, val topicId: UUID? = null)
