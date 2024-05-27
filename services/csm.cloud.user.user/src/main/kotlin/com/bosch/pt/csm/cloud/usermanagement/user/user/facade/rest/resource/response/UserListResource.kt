/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.AbstractPageResource

class UserListResource(
    val users: Collection<UserResource>,
    pageNumber: Int,
    pageSize: Int,
    totalPages: Int,
    totalElements: Long
) : AbstractPageResource(pageNumber, pageSize, totalPages, totalElements)
