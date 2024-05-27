/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.messages

fun UserEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun UserEventAvro.getVersion() = getAggregate().getVersion()

fun UserEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun UserEventAvro.getCreatedByUserIdentifier() = getAggregate().getCreatedByUserIdentifier()

fun UserEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun UserEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun UserEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()
