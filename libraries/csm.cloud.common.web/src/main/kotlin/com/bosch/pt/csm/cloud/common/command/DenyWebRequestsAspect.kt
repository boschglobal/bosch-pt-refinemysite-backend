/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder.getRequestAttributes

@Aspect
@Component
class DenyWebRequestsAspect {

  @Around("@annotation(com.bosch.pt.csm.cloud.common.command.DenyWebRequests)")
  fun assertWebContextInactive(joinPoint: ProceedingJoinPoint): Any? {
    check(!isWebRequest) { "Method must not be called by a Web request." }
    return joinPoint.proceed()
  }

  private val isWebRequest: Boolean
    get() = getRequestAttributes().let { it != null && it !is AsyncRequestScopeAttributes }
}
