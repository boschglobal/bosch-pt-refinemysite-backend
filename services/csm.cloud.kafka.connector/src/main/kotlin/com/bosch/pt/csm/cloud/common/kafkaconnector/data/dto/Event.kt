/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.data.dto

import java.util.UUID

data class Event(
    val id: Long,
    val eventKey: ByteArray,
    val data: ByteArray?,
    val partitionNumber: Int,
    val traceHeaderKey: String?,
    val traceHeaderValue: String?,
    val transactionId: UUID?
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Event

    if (id != other.id) return false
    if (partitionNumber != other.partitionNumber) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + partitionNumber
    return result
  }
}
