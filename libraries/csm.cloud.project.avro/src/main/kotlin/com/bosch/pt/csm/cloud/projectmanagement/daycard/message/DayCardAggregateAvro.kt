/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.daycard.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardAggregateG2Avro

fun DayCardAggregateG2Avro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun DayCardAggregateG2Avro.getTaskIdentifier() = getTask().getIdentifier().toUUID()

fun DayCardAggregateG2Avro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun DayCardAggregateG2Avro.getDayCardVersion() = getAggregateIdentifier().getVersion()

fun DayCardAggregateG2Avro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun DayCardAggregateG2Avro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
