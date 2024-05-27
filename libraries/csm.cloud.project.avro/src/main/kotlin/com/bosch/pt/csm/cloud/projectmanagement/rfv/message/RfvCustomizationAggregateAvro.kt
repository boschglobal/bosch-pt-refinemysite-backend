/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.rfv.message

import com.bosch.pt.csm.cloud.common.extensions.toInstantByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.messages.buildAggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationAggregateAvro

fun RfvCustomizationAggregateAvro.getIdentifier() =
    getAggregateIdentifier().getIdentifier().toUUID()

fun RfvCustomizationAggregateAvro.getVersion() = getAggregateIdentifier().getVersion()

fun RfvCustomizationAggregateAvro.buildAggregateIdentifier() =
    getAggregateIdentifier().buildAggregateIdentifier()

fun RfvCustomizationAggregateAvro.getProjectIdentifier() = getProject().getIdentifier().toUUID()

fun RfvCustomizationAggregateAvro.getCreatedBy() =
    getAuditingInformation().getCreatedBy().getIdentifier().toUUID()

fun RfvCustomizationAggregateAvro.getCreatedDate() =
    getAuditingInformation().getCreatedDate().toInstantByMillis()

fun RfvCustomizationAggregateAvro.getLastModifiedBy() =
    getAuditingInformation().getLastModifiedBy().getIdentifier().toUUID()

fun RfvCustomizationAggregateAvro.getLastModifiedDate() =
    getAuditingInformation().getLastModifiedDate().toInstantByMillis()
