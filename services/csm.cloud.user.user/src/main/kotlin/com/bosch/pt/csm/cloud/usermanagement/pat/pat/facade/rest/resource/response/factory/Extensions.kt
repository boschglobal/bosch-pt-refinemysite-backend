/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.pat.pat.facade.rest.resource.response.factory

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import java.util.Optional

fun referTo(userId: Optional<UserId>): ResourceReference =
  ResourceReference(userId.get().identifier, "User ${userId.get().identifier}")