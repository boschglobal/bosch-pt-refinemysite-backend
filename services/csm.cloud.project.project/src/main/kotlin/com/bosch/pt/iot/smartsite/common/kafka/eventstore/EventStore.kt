/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.kafka.eventstore

import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamable

interface EventStore {

  /**
   * Stores the given event in the database to be published via Kafka upon successful transaction
   * commit.
   *
   * At-least-once delivery is achieved as follows:
   *
   * * If the enclosing transaction ends in a rollback, stored events will also be rolled back. No
   * events will be published to Kafka in case of a rollback.
   * * If the enclosing transaction ends in a commit, stored events become visible to the Kafka
   * Connector service. The Kafka Connector service reads the event from the database, publishes it
   * as a Kafka message and removes the corresponding database entry only when the message has been
   * acknowledged.
   */
  fun save(kafkaStreamable: KafkaStreamable)
}
