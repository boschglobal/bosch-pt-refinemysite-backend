/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.participant.message

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantAggregateG3Avro

fun ParticipantAggregateG3Avro.getIdentifier() = getAggregateIdentifier().getIdentifier().toUUID()

fun ParticipantAggregateG3Avro.getVersion() = getAggregateIdentifier().getVersion()

fun ParticipantAggregateG3Avro.getProjectIdentifier() = getProject().getIdentifier().toUUID()

fun ParticipantAggregateG3Avro.getCompanyIdentifier() = getCompany()?.getIdentifier()?.toUUID()

fun ParticipantAggregateG3Avro.getUserIdentifier() = getUser()?.getIdentifier()?.toUUID()

fun ParticipantAggregateG3Avro.buildCompanyAggregateIdentifier() =
    AggregateIdentifier(
        type = getCompany().getType(),
        identifier = getCompany().getIdentifier().toUUID(),
        version = getCompany().getVersion())

fun ParticipantAggregateG3Avro.buildUserAggregateIdentifier() =
    AggregateIdentifier(
        type = getUser().getType(),
        identifier = getUser().getIdentifier().toUUID(),
        version = getUser().getVersion())
