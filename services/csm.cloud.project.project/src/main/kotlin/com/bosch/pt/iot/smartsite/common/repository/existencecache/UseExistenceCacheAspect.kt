/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository.existencecache

import java.lang.reflect.Parameter
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Aspect
@Component
class UseExistenceCacheAspect(private val cache: ExistenceCache) {

  @Around(
      "@annotation(com.bosch.pt.iot.smartsite.common.repository.existencecache.UseExistenceCache)")
  fun useCacheOrFallBackToRepositoryCall(joinPoint: ProceedingJoinPoint): Any? {

    // This aspect should only be applied to web and kafka requests
    if (!isWebRequestOrKafkaRequestScope) {
      return joinPoint.proceed()
    }

    // Checks if UseExistenceCache is only used on queries returning boolean
    require(joinPoint.getMethodReturnType().typeName == "boolean") {
      "Annotation @${UseExistenceCache::class.java.simpleName} can be used only for methods with boolean return type."
    }

    val annotation = joinPoint.getUseCacheAnnotation()

    return if (cache[
        annotation.cacheName,
        annotation.determineCacheKey(joinPoint.args, joinPoint.getMethodParameters())] != null) {
      true
    } else {
      joinPoint.proceed()
    }
  }

  private fun ProceedingJoinPoint.getMethodReturnType() =
      (this.signature as MethodSignature).returnType

  private fun ProceedingJoinPoint.getMethodParameters(): Array<Parameter> =
      (this.signature as MethodSignature).method.parameters

  private fun ProceedingJoinPoint.getUseCacheAnnotation(): UseExistenceCache =
      (this.signature as MethodSignature).method.getAnnotation(UseExistenceCache::class.java)

  private fun UseExistenceCache.determineCacheKey(
      args: Array<Any>,
      parameters: Array<Parameter>
  ): List<Any> {
    require(this.keyFromParameters.isNotEmpty() && this.keyFromParameters.any { it.isNotEmpty() }) {
      "Found a empty parameter annotated in a function annotated with @${UseExistenceCache::class.java.simpleName}."
    }

    val cacheKey = mutableListOf<Any>()

    keyFromParameters.forEach { k ->
      parameters.mapIndexed { index, parameter ->
        if (k == parameter.name) {
          cacheKey.add(args[index])
        }
      }
    }
    return cacheKey
  }

  private val isWebRequestOrKafkaRequestScope: Boolean
    get() = RequestContextHolder.getRequestAttributes() != null
}
