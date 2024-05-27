/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.rfv.message

import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventAvro

fun RfvCustomizationEventAvro.getIdentifier() = getAggregate().getIdentifier()

fun RfvCustomizationEventAvro.getVersion() = getAggregate().getVersion()

fun RfvCustomizationEventAvro.buildAggregateIdentifier() = getAggregate().buildAggregateIdentifier()

fun RfvCustomizationEventAvro.getProjectIdentifier() = getAggregate().getProjectIdentifier()

fun RfvCustomizationEventAvro.getCreatedBy() = getAggregate().getCreatedBy()

fun RfvCustomizationEventAvro.getCreatedDate() = getAggregate().getCreatedDate()

fun RfvCustomizationEventAvro.getLastModifiedBy() = getAggregate().getLastModifiedBy()

fun RfvCustomizationEventAvro.getLastModifiedDate() = getAggregate().getLastModifiedDate()
