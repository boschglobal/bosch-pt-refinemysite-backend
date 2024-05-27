/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

import com.bosch.pt.csm.cloud.common.exceptions.UserNotRegisteredException
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.UnregisteredUser
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class RegisteredUserPrincipalResolver : HandlerMethodArgumentResolver {
  override fun supportsParameter(parameter: MethodParameter): Boolean =
      findMethodAnnotation(RegisteredUserPrincipal::class.java, parameter) != null

  override fun resolveArgument(
      parameter: MethodParameter,
      mavContainer: ModelAndViewContainer?,
      webRequest: NativeWebRequest,
      binderFactory: WebDataBinderFactory?
  ): Any? =
      SecurityContextHolder.getContext().authentication?.let {
        val principal = it.principal
        return principal as? User
            ?: if (principal is UnregisteredUser) {
              throw UserNotRegisteredException()
            } else {
              throw IllegalStateException("Unknown principal type received from security context")
            }
      }
}
