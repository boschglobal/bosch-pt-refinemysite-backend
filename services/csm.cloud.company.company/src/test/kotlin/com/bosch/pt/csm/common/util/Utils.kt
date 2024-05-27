/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.common.util

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase

fun randomUUID(): UUID = UUID.randomUUID()

fun String.toUUID(): UUID = UUID.fromString(this)

fun SpecificRecordBase.getIdentifier(): UUID =
    UUID.fromString(
        (this.get("aggregateIdentifier") as SpecificRecordBase).get("identifier") as String)

fun SpecificRecordBase.getCreatedBy(): AggregateIdentifierAvro =
    (this.get("auditingInformation") as SpecificRecordBase).get("createdBy") as
        AggregateIdentifierAvro

fun SpecificRecordBase.getLastModifiedBy(): AggregateIdentifierAvro =
    (this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedBy") as
        AggregateIdentifierAvro

fun SpecificRecordBase.getCreatedDate(): LocalDateTime =
    ((this.get("auditingInformation") as SpecificRecordBase).get("createdDate") as Long)
        .toLocalDateTimeByMillis()

fun SpecificRecordBase.getLastModifiedDate(): LocalDateTime =
    ((this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedDate") as Long)
        .toLocalDateTimeByMillis()
