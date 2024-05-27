/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.request

import com.bosch.pt.csm.cloud.common.facade.rest.resource.validation.StringEnumeration
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import jakarta.validation.constraints.Size

/** Resource for creating an [Topic]. */
class CreateTopicResource(
    @field:Size(max = Topic.MAX_DESCRIPTION_LENGTH) val description: String?,
    @field:StringEnumeration(
        enumClass = TopicCriticalityEnum::class, enumValues = TopicCriticalityEnum.ENUM_VALUES)
    val criticality: TopicCriticalityEnum
)
