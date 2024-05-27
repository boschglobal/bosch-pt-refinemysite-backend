/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.milestone.message

import com.bosch.pt.csm.cloud.projectmanagement.milestone.messages.MilestoneEventAvro

fun MilestoneEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun MilestoneEventAvro.getVersion() = getAggregate().getVersion()

fun MilestoneEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()

fun MilestoneEventAvro.getProjectIdentifier() = getAggregate().getProjectIdentifier()

fun MilestoneEventAvro.getCraftIdentifier() = getAggregate().getCraftIdentifier()

fun MilestoneEventAvro.getWorkAreaIdentifier() = getAggregate().getWorkAreaIdentifier()

fun MilestoneEventAvro.getCreatedBy() = getAggregate().getCreatedBy()

fun MilestoneEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun MilestoneEventAvro.getLastModifiedBy() = getAggregate().getLastModifiedBy()

fun MilestoneEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()
