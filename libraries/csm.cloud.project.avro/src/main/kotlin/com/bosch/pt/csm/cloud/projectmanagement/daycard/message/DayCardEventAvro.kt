/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.daycard.message

import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventG2Avro
import java.util.UUID

fun DayCardEventG2Avro.getIdentifier(): UUID = getAggregate().getIdentifier()

fun DayCardEventG2Avro.getVersion() = getAggregate().getDayCardVersion()

fun DayCardEventG2Avro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun DayCardEventG2Avro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun DayCardEventG2Avro.getTaskIdentifier() = getAggregate().getTaskIdentifier()

fun DayCardEventG2Avro.name() = getName().name
