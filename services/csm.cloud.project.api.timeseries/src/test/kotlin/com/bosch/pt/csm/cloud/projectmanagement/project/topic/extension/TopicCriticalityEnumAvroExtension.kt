/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.extension

import com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model.TopicCriticalityEnum
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicCriticalityEnumAvro

fun TopicCriticalityEnumAvro.asCriticality() = TopicCriticalityEnum.valueOf(this.name)
