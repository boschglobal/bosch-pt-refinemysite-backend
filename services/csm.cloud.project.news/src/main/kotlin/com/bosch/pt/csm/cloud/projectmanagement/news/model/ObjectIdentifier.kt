/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.model

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import java.util.Objects
import java.util.UUID
import jakarta.persistence.Embeddable

@Embeddable
open class ObjectIdentifier(val type: String, val identifier: UUID) {

  constructor(
      aggregateIdentifierAvro: AggregateIdentifierAvro
  ) : this(aggregateIdentifierAvro.type, aggregateIdentifierAvro.identifier.toUUID())

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ObjectIdentifier
    return type == that.type && identifier == that.identifier
  }

  override fun hashCode() = Objects.hash(type, identifier)
}
