/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.kafka

import org.slf4j.Logger
import org.springframework.kafka.KafkaException.Level.TRACE
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.kafka.support.KafkaUtils
import org.springframework.util.backoff.BackOff
import org.springframework.util.backoff.ExponentialBackOff

object KafkaConsumerConfigUtil {

  /**
   * Configures a [DefaultErrorHandler] with a retry listener which logs an error every 5 attempts.
   *
   * @param logger the logger used in retry listener
   * @param backOff the [BackOff] (defaults to [ExponentialBackOff])
   */
  fun configureDefaultErrorHandler(
      logger: Logger,
      backOff: BackOff = ExponentialBackOff(),
  ): CommonErrorHandler {
    return DefaultErrorHandler(backOff).apply {
      setLogLevel(TRACE)
      setRetryListeners(
          RetryListener { record, exception, deliveryAttempt ->
            val message =
                """
                        Consuming Kafka message failed. Current retry attempt: %s
                            Failed record: %s
                        Cause: %s
                      """
                    .trimIndent()
                    .format(
                        deliveryAttempt,
                        KafkaUtils.format(record),
                        exception.stackTraceToString(),
                    )

            if (deliveryAttempt % 5 == 0) logger.error(message) else logger.debug(message)
          })
    }
  }
}
