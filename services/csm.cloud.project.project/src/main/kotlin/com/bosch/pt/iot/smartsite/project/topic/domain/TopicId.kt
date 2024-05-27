/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.domain

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.csm.cloud.projectmanagement.common.ProjectmanagementAggregateTypeEnum.TOPIC
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID
import java.util.UUID.randomUUID

@Embeddable
data class TopicId(@get:JsonValue override val identifier: UUID = randomUUID()) :
    Serializable, UuidIdentifiable {

  constructor(identifier: String) : this(identifier.toUUID())

  override fun toString(): String = identifier.toString()

  companion object {
    private const val serialVersionUID: Long = -134866597220482783
  }
}

fun UUID.asTopicId() = TopicId(this)

fun String.asTopicId() = TopicId(this)

fun Set<UUID>.asTopicIds() = this.map { it.asTopicId() }.toSet()

fun Set<TopicId>.asUuidIds() = this.map { it.toUuid() }.toSet()

fun TopicId.toAggregateReference(): AggregateIdentifierAvro =
    AggregateIdentifierAvro.newBuilder()
        .setType(TOPIC.name)
        .setIdentifier(this.identifier.toString())
        .setVersion(0)
        .build()
