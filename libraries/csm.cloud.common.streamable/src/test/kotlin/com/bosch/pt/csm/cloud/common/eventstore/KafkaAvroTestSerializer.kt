/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.eventstore

import java.io.Serializable
import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.common.serialization.Serializer

class KafkaAvroTestSerializer : Serializer<Any?> {

  override fun configure(configs: Map<String?, *>?, isKey: Boolean) = Unit

  override fun serialize(topic: String, data: Any?): ByteArray =
      SerializationUtils.serialize(data as Serializable?)

  override fun close() = Unit
}
