/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.monitoring.bam.importer.tables

import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase

internal fun extractUserIdOfActingUser(event: GenericRecord): String =
    if (event.hasField("auditingInformation"))
        event.extract("auditingInformation")["user"].toString()
    else if (event.hasField("aggregate"))
        event
            .extract("aggregate")
            .extract("auditingInformation")
            .extract("lastModifiedBy")["identifier"]
            .toString()
    else "unknown"

internal fun GenericRecord.extract(field: String) = this[field] as GenericRecord

internal fun SpecificRecordBase.extract(field: String) = this[field] as SpecificRecordBase
