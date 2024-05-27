/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.util

import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro

fun UserAggregateAvro.displayName() = "${getFirstName()} ${getLastName()}"
