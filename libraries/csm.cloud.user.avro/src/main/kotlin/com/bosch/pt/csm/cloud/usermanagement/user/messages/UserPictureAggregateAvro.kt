/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.user.messages

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier

fun UserPictureAggregateAvro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun UserPictureAggregateAvro.getUserIdentifier() = getUser().getIdentifier().toUUID()

fun UserPictureAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun UserPictureAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun UserPictureAggregateAvro.getCreatedByUserIdentifier() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun UserPictureAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun UserPictureAggregateAvro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun UserPictureAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
