/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.factory.PageLinks
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USERS_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserListResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class UserListResourceFactory(
    private val userResourceFactoryHelper: UserResourceFactoryHelper,
    private val linkFactory: CustomLinkBuilderFactory
) {

  @PageLinks
  fun build(users: Page<User>): UserListResource {
    val userResources = userResourceFactoryHelper.buildUserResources(users.content)

    val resource =
        UserListResource(
            userResources, users.number, users.size, users.totalPages, users.totalElements)

    // Add self reference
    resource.add(linkFactory.linkTo(USERS_ENDPOINT_PATH).withSelfRel())

    return resource
  }
}
