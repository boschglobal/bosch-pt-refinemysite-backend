/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.dto

data class GenericPageListWrapper<T>(
    val resources: List<T>? = null,
    val previous: Boolean = false
)
