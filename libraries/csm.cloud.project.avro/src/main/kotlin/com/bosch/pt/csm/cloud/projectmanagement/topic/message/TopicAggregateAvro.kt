/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.topic.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.topic.messages.TopicAggregateG2Avro

fun TopicAggregateG2Avro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun TopicAggregateG2Avro.getTaskIdentifier() = getTask().getIdentifier().toUUID()

fun TopicAggregateG2Avro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()

fun TopicAggregateG2Avro.getLastModifiedByUserIdentifier() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun TopicAggregateG2Avro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()
