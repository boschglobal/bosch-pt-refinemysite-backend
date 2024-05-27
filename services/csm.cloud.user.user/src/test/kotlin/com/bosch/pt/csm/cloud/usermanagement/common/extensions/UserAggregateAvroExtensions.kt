/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.common.extensions

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier

fun UserAggregateAvro.toUserId() = UserId(this.getIdentifier())
