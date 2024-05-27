/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.participant.message

import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationEventAvro

fun InvitationEventAvro.getIdentifier() = getAggregate().getIdentifier()