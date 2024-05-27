/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ListResponseResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.UserController.Companion.USER_SUGGESTIONS_ENDPOINT_PATH
import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserReference
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class UserReferencePageFactory(private val linkFactory: CustomLinkBuilderFactory) {

  fun buildForSuggestions(users: Page<User>): ListResponseResource<UserReference> =
      users.content.map(UserReference::from).let {
        ListResponseResource(it, users.number, users.size, users.totalPages, users.totalElements)
            .apply { add(linkFactory.linkTo(USER_SUGGESTIONS_ENDPOINT_PATH).withSelfRel()) }
      }
}
