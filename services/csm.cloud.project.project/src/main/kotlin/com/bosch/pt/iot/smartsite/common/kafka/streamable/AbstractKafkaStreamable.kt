/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.streamable

import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import java.io.Serializable
import jakarta.persistence.Transient

abstract class AbstractKafkaStreamable<T : Serializable, U : AbstractEntity<T, U>, V : Enum<*>> :
    AbstractEntity<T, U>(), KafkaStreamable {

  @Transient
  var eventType: V? = null
    set(value) {
      if (field == null) {
        field = value
      } else {
        throw IllegalStateException("eventType is already set")
      }
    }
}
