/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.util

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.AggregateIdentifierAvro
import com.bosch.pt.iot.smartsite.common.util.TimeUtilities.asLocalDateTime
import java.time.LocalDateTime
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase

fun SpecificRecordBase.getIdentifier(): UUID =
    ((this.get("aggregateIdentifier") as SpecificRecordBase).get("identifier") as String).toUUID()

fun SpecificRecordBase.getCreatedBy(): AggregateIdentifierAvro =
    (this.get("auditingInformation") as SpecificRecordBase).get("createdBy") as
        AggregateIdentifierAvro

fun SpecificRecordBase.getLastModifiedBy(): AggregateIdentifierAvro =
    (this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedBy") as
        AggregateIdentifierAvro

fun SpecificRecordBase.getCreatedDate(): LocalDateTime =
    asLocalDateTime(
        (this.get("auditingInformation") as SpecificRecordBase).get("createdDate") as Long)

fun SpecificRecordBase.getLastModifiedDate(): LocalDateTime =
    asLocalDateTime(
        (this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedDate") as Long)
