/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.milestone.message

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneListEventAvro

fun MilestoneListEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun MilestoneListEventAvro.getVersion() = getAggregate().getVersion()

fun MilestoneListEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()

fun MilestoneListEventAvro.getProjectIdentifier() = getAggregate().getProjectIdentifier()

fun MilestoneListEventAvro.getWorkAreaIdentifier() = getAggregate().getWorkAreaIdentifier()

fun MilestoneListEventAvro.getCreatedBy() = getAggregate().getCreatedBy()

fun MilestoneListEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun MilestoneListEventAvro.getLastModifiedBy() = getAggregate().getLastModifiedBy()

fun MilestoneListEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()
