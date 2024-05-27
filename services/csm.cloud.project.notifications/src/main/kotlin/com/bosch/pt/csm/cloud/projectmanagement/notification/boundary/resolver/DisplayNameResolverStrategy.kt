/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.resolver

import com.bosch.pt.csm.cloud.projectmanagement.notification.model.ObjectReferenceWithContextRoot

interface DisplayNameResolverStrategy {
    val type: String
    fun getDisplayName(objectReference: ObjectReferenceWithContextRoot): String
}
