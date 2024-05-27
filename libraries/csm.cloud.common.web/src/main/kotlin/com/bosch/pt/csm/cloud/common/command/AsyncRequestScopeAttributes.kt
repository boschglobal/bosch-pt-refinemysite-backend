/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.common.command

import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes
import org.springframework.web.context.request.RequestContextHolder.setRequestAttributes

class AsyncRequestScopeAttributes : RequestAttributes {

  private val requestAttributeMap: MutableMap<String, Any> = HashMap()

  override fun getAttribute(name: String, scope: Int): Any? =
      if (scope == SCOPE_REQUEST) {
        requestAttributeMap[name]
      } else null

  override fun setAttribute(name: String, value: Any, scope: Int) {
    if (scope == SCOPE_REQUEST) {
      requestAttributeMap[name] = value
    }
  }

  override fun removeAttribute(name: String, scope: Int) {
    if (scope == SCOPE_REQUEST) {
      requestAttributeMap.remove(name)
    }
  }

  override fun getAttributeNames(scope: Int): Array<String?> =
      if (scope == SCOPE_REQUEST) {
        requestAttributeMap.keys.toTypedArray()
      } else arrayOfNulls(0)

  // Not supported
  override fun registerDestructionCallback(name: String, callback: Runnable, scope: Int) {
    // Do nothing
  }

  // Not supported
  override fun resolveReference(key: String): Any? = null

  // Not supported
  override fun getSessionId(): String {
    return ""
  }

  // Not supported
  override fun getSessionMutex(): Any {
    return Any()
  }

  companion object {
    fun executeWithAsyncRequestScope(block: Runnable) {
      try {
        setRequestAttributes(AsyncRequestScopeAttributes())
        block.run()
      } finally {
        resetRequestAttributes()
      }
    }
  }
}
