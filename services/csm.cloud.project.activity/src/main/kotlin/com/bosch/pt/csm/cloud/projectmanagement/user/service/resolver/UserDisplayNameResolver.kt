/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.user.service.resolver

import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.USER_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.common.service.resolver.DisplayNameResolver
import com.bosch.pt.csm.cloud.projectmanagement.user.repository.UserRepository
import com.bosch.pt.csm.cloud.projectmanagement.user.service.UserService
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class UserDisplayNameResolver(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val messageSource: MessageSource
) : DisplayNameResolver {

  override val type: String = "USER"

  override fun getDisplayName(objectReference: UnresolvedObjectReference): String {
    val user = userService.findOneCached(objectReference.identifier)

    return user?.let { userRepository.findDisplayNameCached(user.identifier) }
        ?: messageSource.getMessage(USER_DELETED, null, LocaleContextHolder.getLocale())
  }
}
