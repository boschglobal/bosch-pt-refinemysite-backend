/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request

import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.TopicCriticalityEnum

class CreateTopicResource(
    val description: String? = null,
    val criticality: TopicCriticalityEnum? = null
)
