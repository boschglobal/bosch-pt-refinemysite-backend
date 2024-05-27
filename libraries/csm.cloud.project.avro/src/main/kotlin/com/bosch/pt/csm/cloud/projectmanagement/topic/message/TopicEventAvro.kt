/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.topic.message

import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicEventG2Avro

fun TopicEventG2Avro.getIdentifier() = getAggregate().getIdentifier()

fun TopicEventG2Avro.getLastModifiedDate() = getAggregate().getLastModifiedDate()

fun TopicEventG2Avro.getLastModifiedByUserIdentifier() =
    getAggregate().getLastModifiedByUserIdentifier()
