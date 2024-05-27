/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.projectstatistics.model

import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum

class TopicCriticalityStatisticsEntry(
    count: Long,
    property: TopicCriticalityEnum,
    entityIdentifier: ProjectId
) : StatisticsEntry<TopicCriticalityEnum, Long>(count, property, entityIdentifier)
