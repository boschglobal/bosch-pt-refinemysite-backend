/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization.cache

import com.bosch.pt.csm.cloud.common.api.UuidIdentifiable
import com.bosch.pt.iot.smartsite.common.repository.AbstractCache
import java.util.UUID
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

@Aspect
@Component
class InvalidateAuthorizationCacheAspect(
    private val authorizationCaches: List<AbstractCache<UUID, *>>
) {

  @Around(
      "@annotation(com.bosch.pt.iot.smartsite.common.authorization.cache.InvalidatesAuthorizationCache)")
  fun invalidateAuthorizationCache(
      joinPoint: ProceedingJoinPoint,
  ): Any? {
    val result = joinPoint.proceed()
    if (isRequestScopeAvailable()) {
      val (indexOfAnnotatedParameter, annotation) =
          joinPoint.getParameterAnnotation(AuthorizationCacheKey::class.java)
              ?: throw IllegalArgumentException(
                  "A method annotated with @${InvalidatesAuthorizationCache::class.java.simpleName} must have " +
                      "exactly one parameter annotated with @${AuthorizationCacheKey::class.java.simpleName}.")

      val annotatedArg = joinPoint.args[indexOfAnnotatedParameter]
      if (annotatedArg != null) {
        val cacheKeys = determineCacheKeys(annotatedArg, annotation!!)
        cacheKeys.forEach { key -> authorizationCaches.forEach { it.invalidateCacheKey(key) } }
      }
    }
    return result
  }

  private fun determineCacheKeys(arg: Any, annotation: AuthorizationCacheKey): List<UUID> =
      when (arg) {
        is Collection<*> -> determineCacheKeysFromCollection(arg, annotation)
        is UuidIdentifiable -> listOf(arg.toUuid())
        is UUID -> listOf(arg)
        else -> listOfNotNull(determineCacheKeyUsingSpEl(arg, annotation))
      }

  private fun determineCacheKeysFromCollection(
      arg: Collection<*>,
      annotation: AuthorizationCacheKey
  ): List<UUID> =
      arg.stream()
          .map { determineCacheKeys(it!!, annotation) }
          .flatMap { it.stream() }
          .toList()
          .filterNotNull()

  private fun determineCacheKeyUsingSpEl(arg: Any, annotation: AuthorizationCacheKey): UUID? {
    require(annotation.field.isNotBlank()) {
      "Found a parameter annotated with @${AuthorizationCacheKey::class.java.simpleName} that is " +
          "neither a UUID nor a collection of UUIDs. In this case, the \"value\" must be set on the annotation."
    }
    val result =
        annotation.field.toSpelExpression().evaluateOn(arg).let {
          if (it is UuidIdentifiable) it.toUuid() else it
        }

    check(result is UUID?) {
      "Could not extract cache key from argument $arg using SpEL expression \"${annotation.field}\". " +
          "Teh evaluation returned $result but an object of type UUID was expected."
    }
    return result
  }

  private fun isRequestScopeAvailable() = RequestContextHolder.getRequestAttributes() != null

  private fun <T : Annotation> ProceedingJoinPoint.getParameterAnnotation(
      annotationClass: Class<T>
  ): Pair<Int, T?>? =
      (this.signature as MethodSignature)
          .method
          .parameters
          .mapIndexed { index, annotation ->
            Pair(index, annotation.getAnnotation(annotationClass))
          }
          .singleOrNull { (_, annotation) -> annotation != null }

  private fun String.toSpelExpression() = SpelExpressionParser().parseExpression(this)

  private fun Expression.evaluateOn(evaluationRoot: Any) = this.getValue(evaluationRoot)
}
