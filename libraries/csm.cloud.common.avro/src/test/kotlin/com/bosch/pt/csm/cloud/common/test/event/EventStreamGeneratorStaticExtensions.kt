/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.test.event

import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase

class EventStreamGeneratorStaticExtensions private constructor() {

  companion object {
    private var eventStreamGenerator: EventStreamGenerator? = null

    fun init(eventStreamGenerator: EventStreamGenerator) {
      Companion.eventStreamGenerator = eventStreamGenerator
    }

    @JvmStatic
    fun getActiveUserFromContext(): AggregateIdentifierAvro =
        eventStreamGenerator!!.getActiveUserFromContext()

    @JvmStatic
    fun getByReference(reference: String): AggregateIdentifierAvro =
        eventStreamGenerator!!.getByReference(reference)

    @JvmStatic
    fun getIdentifier(reference: String): UUID = eventStreamGenerator!!.getIdentifier(reference)

    @JvmStatic
    fun <T : SpecificRecordBase> get(reference: String) = eventStreamGenerator!!.get<T>(reference)
  }
}

fun EventStreamGenerator.registerStaticContext(): EventStreamGenerator {
  EventStreamGeneratorStaticExtensions.init(this)
  return this
}
