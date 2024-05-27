/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.external.message

import com.bosch.pt.csm.cloud.projectmanagement.external.messages.ExternalIdEventAvro

fun ExternalIdEventAvro.getIdentifier() = this.aggregate.getIdentifier()

fun ExternalIdEventAvro.getVersion() = this.aggregate.getVersion()

fun ExternalIdEventAvro.getProjectIdentifier() = this.aggregate.getProjectIdentifier()

fun ExternalIdEventAvro.getCreatedBy() = this.aggregate.getCreatedBy()

fun ExternalIdEventAvro.getCreatedDate() = this.aggregate.getCreatedDate()

fun ExternalIdEventAvro.getLastModifiedBy() = this.aggregate.getLastModifiedBy()

fun ExternalIdEventAvro.getLastModifiedDate() = this.aggregate.getLastModifiedDate()

fun ExternalIdEventAvro.buildAggregateIdentifier() = this.aggregate.buildAggregateIdentifier()
