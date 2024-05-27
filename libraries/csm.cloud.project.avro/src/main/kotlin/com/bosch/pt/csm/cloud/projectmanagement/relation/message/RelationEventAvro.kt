/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.relation.message

import com.bosch.pt.csm.cloud.projectmanagement.relation.messages.RelationEventAvro

fun RelationEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun RelationEventAvro.getVersion() = getAggregate().getVersion()

fun RelationEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()

fun RelationEventAvro.getProjectIdentifier() = getAggregate().getProjectIdentifier()

fun RelationEventAvro.getSourceIdentifier() = getAggregate().getSource()

fun RelationEventAvro.getTargetIdentifier() = getAggregate().getTarget()

fun RelationEventAvro.getCreatedBy() = getAggregate().getCreatedBy()

fun RelationEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun RelationEventAvro.getLastModifiedBy() = getAggregate().getLastModifiedBy()

fun RelationEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()
