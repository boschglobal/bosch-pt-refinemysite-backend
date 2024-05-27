/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.facade.listener.message

import com.bosch.pt.csm.cloud.projectmanagement.user.model.GenderEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import com.bosch.pt.csm.cloud.usermanagement.user.messages.UserAggregateAvro
import com.bosch.pt.csm.cloud.usermanagement.user.messages.getIdentifier
import org.apache.commons.lang3.LocaleUtils

fun UserAggregateAvro.toEntity() =
    User(
        identifier = getIdentifier(),
        externalIdentifier = getUserId(),
        displayName = "${getFirstName()} ${getLastName()}",
        gender = getGender()?.run { GenderEnum.valueOf(name) },
        locale = getLocale()?.let { LocaleUtils.toLocale(it) },
        admin = getAdmin(),
        locked = getLocked())
