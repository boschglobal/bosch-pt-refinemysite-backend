/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafkaconnector.kafka.serializer

import org.apache.kafka.common.serialization.Serializer

class NoOpSerializer : Serializer<Any?> {

  override fun configure(configs: Map<String?, *>?, isKey: Boolean) {
    // do nothing
  }

  override fun close() {
    // do nothing
  }

  override fun serialize(topic: String, data: Any?): ByteArray? =
      when (data) {
        is ByteArray -> data
        // Sonarqube reported a code smell here. This was a false positive.
        // The method needs to return null and NOT an empty byte array because
        // empty byte arrays aren't detected as tombstone messages!
        null -> null
        else ->
            throw IllegalArgumentException(
                "NoOpSerializer was called with anything else then byte[].")
      }
}
