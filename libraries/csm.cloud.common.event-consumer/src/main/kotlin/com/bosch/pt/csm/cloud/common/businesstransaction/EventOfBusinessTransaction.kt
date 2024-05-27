/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction

import java.time.LocalDateTime
import java.util.UUID

data class EventOfBusinessTransaction(

    /**
     * the date and time this [EventOfBusinessTransaction] was created (this is *not* the time the
     * event occurred)
     */
    val creationDate: LocalDateTime,

    /**
     * the date and time this event was published as a message (this is the Kafka timestamp, it's
     * *not* the time the event occurred)
     */
    val messageDate: LocalDateTime,

    /** the offset of the Kafka message representing the event */
    val offset: Long,

    /** the identifier of the enclosing business transaction */
    val transactionIdentifier: UUID,

    /** the Kafka message key of this event */
    val eventKey: ByteArray,

    /** the Kafka message value of this event */
    val eventValue: ByteArray,

    /** the fully qualified class name of the eventKey's Avro type */
    val eventKeyClass: String,

    /** the fully qualified class name of the eventValue's Avro type */
    val eventValueClass: String,

    /**
     * the name of the event processor that is in charge of managing this event, i.e. saving the
     * event, and deleting it after the processing is done by the event processor. This event must
     * not be accessed by any other event processor.
     */
    val eventProcessorName: String
)
