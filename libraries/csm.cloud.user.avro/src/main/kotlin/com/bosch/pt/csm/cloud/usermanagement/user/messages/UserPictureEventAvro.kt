/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.messages

import com.bosch.pt.csm.cloud.common.extensions.toUUID

fun UserPictureEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun UserPictureEventAvro.getUserIdentifier() = getAggregate().getUser().getIdentifier().toUUID()

fun UserPictureEventAvro.getVersion() = getAggregate().getVersion()

fun UserPictureEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun UserPictureEventAvro.getCreatedByUserIdentifier() = getAggregate().getCreatedByUserIdentifier()

fun UserPictureEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun UserPictureEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()

fun UserPictureEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()
