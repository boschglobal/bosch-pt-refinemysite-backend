/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.participant.message

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.participant.messages.InvitationAggregateAvro

fun InvitationAggregateAvro.getIdentifier() = this.getAggregateIdentifier().getIdentifier().toUUID()
