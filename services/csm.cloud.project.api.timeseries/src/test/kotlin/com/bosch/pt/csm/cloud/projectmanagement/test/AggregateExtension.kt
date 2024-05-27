/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.test

import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import org.apache.avro.specific.SpecificRecordBase

fun SpecificRecordBase.eventDate() =
    ((this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedDate") as Long)
        .toLocalDateTimeByMillis()
        .toString()

fun SpecificRecordBase.eventTimestamp() =
    (this.get("auditingInformation") as SpecificRecordBase).get("lastModifiedDate") as Long
