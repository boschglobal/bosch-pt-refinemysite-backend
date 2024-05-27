/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.message.message

import com.bosch.pt.csm.cloud.projectmanagement.message.messages.MessageEventAvro

fun MessageEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun MessageEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun MessageEventAvro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()
