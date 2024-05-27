/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.usermanagement.user.user.facade.rest.resource.response.UserResource
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class UserResourceFactory(private val userResourceFactoryHelper: UserResourceFactoryHelper) {

  fun build(user: User): UserResource =
      userResourceFactoryHelper.buildUserResources(listOf(user)).iterator().next()

  companion object {
    const val EMBEDDED_NAME_PROFILE_PICTURE = "profilePicture"
  }
}
