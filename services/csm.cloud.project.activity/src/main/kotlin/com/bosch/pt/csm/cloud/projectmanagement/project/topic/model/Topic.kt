/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.PROJECT_STATE
import com.bosch.pt.csm.cloud.projectmanagement.project.common.model.ShardedByProjectIdentifier
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(PROJECT_STATE)
@TypeAlias("Topic")
class Topic(
    @Id val identifier: AggregateIdentifier,
    override val projectIdentifier: UUID,
    val criticality: TopicCriticalityEnum,
    val taskIdentifier: UUID,
    val description: String? = null
) : ShardedByProjectIdentifier

enum class TopicCriticalityEnum {
  CRITICAL,
  UNCRITICAL
}
