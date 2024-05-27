/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.user.extension

import com.bosch.pt.csm.cloud.projectmanagement.user.user.query.model.PhoneNumberTypeEnum
import com.bosch.pt.csm.cloud.usermanagement.common.messages.PhoneNumberTypeEnumAvro

fun PhoneNumberTypeEnumAvro.asNumberType() = PhoneNumberTypeEnum.valueOf(this.name)
