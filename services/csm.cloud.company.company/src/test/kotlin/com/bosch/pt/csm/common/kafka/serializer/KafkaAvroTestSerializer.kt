/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.common.kafka.serializer

import java.io.Serializable
import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Serializer

class KafkaAvroTestSerializer : Serializer<Any> {
  override fun configure(configs: Map<String?, *>, isKey: Boolean) {
    // nothing to be configured
  }

  override fun serialize(topic: String, data: Any): ByteArray {
    return SerializationUtils.serialize(data as Serializable)
  }

  override fun close() {
    // nothing to be done on this method call
  }
}
