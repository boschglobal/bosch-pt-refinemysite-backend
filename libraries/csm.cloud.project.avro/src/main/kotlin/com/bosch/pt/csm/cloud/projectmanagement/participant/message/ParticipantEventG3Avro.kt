/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.participant.message

import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.ParticipantEventG3Avro

fun ParticipantEventG3Avro.getIdentifier() = getAggregate().getIdentifier()

fun ParticipantEventG3Avro.getVersion() = getAggregate().getVersion()

fun ParticipantEventG3Avro.getProjectIdentifier() = getAggregate().getProjectIdentifier()

fun ParticipantEventG3Avro.getCompanyIdentifier() = getAggregate().getCompanyIdentifier()

fun ParticipantEventG3Avro.getUserIdentifier() = getAggregate().getUserIdentifier()

fun ParticipantEventG3Avro.buildCompanyAggregateIdentifier() =
    getAggregate().buildCompanyAggregateIdentifier()

fun ParticipantEventG3Avro.buildUserAggregateIdentifier() =
    getAggregate().buildUserAggregateIdentifier()
