/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.businesstransaction.jpa

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import com.bosch.pt.csm.cloud.common.model.converter.UuidConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "event_of_business_transaction",
    indexes =
        [
            Index(
                name = "IX_EventOfBusinessTransaction_TidEventProcessor",
                columnList = "transactionIdentifier,eventProcessorName",
                unique = false),
        ])
class EventOfBusinessTransactionEntity(

    // creationDate
    @Column(nullable = false) var creationDate: LocalDateTime,

    // messageDate
    @Column(nullable = false) var messageDate: LocalDateTime,

    // consumerOffset
    // Note: "offset" is a reserved keyword in H2
    @Column(nullable = false) var consumerOffset: Long,

    // transactionIdentifier
    @Convert(converter = UuidConverter::class)
    @Column(nullable = false, length = 36)
    var transactionIdentifier: UUID,

    // eventKey
    @Lob @Column(nullable = false, columnDefinition = "blob") var eventKey: ByteArray,

    // eventValue
    @Lob @Column(nullable = false, columnDefinition = "blob") var eventValue: ByteArray,

    // eventKeyClass
    @Column(nullable = false) var eventKeyClass: String,

    // eventValueClass
    @Column(nullable = false) var eventValueClass: String,

    // eventProcessorName
    @Column(nullable = false, length = 50) var eventProcessorName: String
) : AbstractPersistable<Long>()
