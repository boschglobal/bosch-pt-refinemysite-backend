/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference

interface DisplayNameResolver {
  val type: String
  fun getDisplayName(objectReference: UnresolvedObjectReference): String
}
