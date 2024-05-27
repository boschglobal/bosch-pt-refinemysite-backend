/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.message

import com.bosch.pt.csm.cloud.projectmanagement.taskconstraint.messages.TaskConstraintCustomizationEventAvro

fun TaskConstraintCustomizationEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun TaskConstraintCustomizationEventAvro.getVersion() = getAggregate().getVersion()

fun TaskConstraintCustomizationEventAvro.buildAggregateIdentifier() =
    getAggregate().buildAggregateIdentifier()

fun TaskConstraintCustomizationEventAvro.getProjectIdentifier() =
    getAggregate().getProjectIdentifier()

fun TaskConstraintCustomizationEventAvro.getCreatedBy() = getAggregate().getCreatedBy()

fun TaskConstraintCustomizationEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun TaskConstraintCustomizationEventAvro.getLastModifiedBy() = getAggregate().getLastModifiedBy()

fun TaskConstraintCustomizationEventAvro.getLastModifiedDate() =
    getAggregate().getLastModifiedDate()
