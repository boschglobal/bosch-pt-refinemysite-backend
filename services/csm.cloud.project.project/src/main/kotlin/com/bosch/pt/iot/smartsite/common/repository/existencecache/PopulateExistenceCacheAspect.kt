/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.repository.existencecache

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractEntity
import com.bosch.pt.iot.smartsite.common.model.AbstractReplicatedEntity
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.context.request.RequestContextHolder

@Aspect
@Component
class PopulateExistenceCacheAspect(private val cache: ExistenceCache) {

  @Around(
      "@annotation(com.bosch.pt.iot.smartsite.common.repository.existencecache.PopulateExistenceCache)")
  fun populateCacheAfterRepositoryCall(joinPoint: ProceedingJoinPoint): Any? {

    val result = joinPoint.proceed()

    // This aspect should only be applied to web and kafka requests
    if (!isWebRequestOrKafkaRequestScope) {
      return result
    }

    // Clear cache after transaction end
    TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
          override fun afterCompletion(status: Int) {
            cache.clear()
          }
        })

    val annotation = joinPoint.getPopulateCacheAnnotation()

    // Populates the cache
    populateCacheFromResult(result, annotation)

    return result
  }

  private fun populateCacheFromResult(result: Any?, annotation: PopulateExistenceCache) {
    when (result) {
      is Collection<*> -> result.forEach { populateCacheFromResult(it, annotation) }
      is AbstractSnapshotEntity<*, *> ->
          cache.put(
              annotation.cacheName,
              annotation.determineCacheKeyUsingSpEl(result),
              result.identifier)
      is AbstractEntity<*, *> ->
          cache.put(
              annotation.cacheName,
              annotation.determineCacheKeyUsingSpEl(result),
              result.identifier!!)
      is AbstractReplicatedEntity<*> ->
          cache.put(
              annotation.cacheName,
              annotation.determineCacheKeyUsingSpEl(result),
              result.identifier!!)
      else ->
          throw IllegalArgumentException(
              "A method annotated with @${PopulateExistenceCache::class.java.simpleName} " +
                  "must return a supported entity to populate.")
    }
  }

  private fun ProceedingJoinPoint.getPopulateCacheAnnotation(): PopulateExistenceCache =
      (this.signature as MethodSignature).method.getAnnotation(PopulateExistenceCache::class.java)

  private fun PopulateExistenceCache.determineCacheKeyUsingSpEl(arg: Any): List<Any> {
    require(this.keyFromResult.isNotEmpty() && this.keyFromResult.any { it.isNotEmpty() }) {
      "Found a parameter annotated with @${PopulateExistenceCache::class.java.simpleName} that is empty."
    }
    return this.keyFromResult.map { it.toSpelExpression().evaluateOn(arg)!! }
  }

  private fun String.toSpelExpression() = SpelExpressionParser().parseExpression(this)

  private fun Expression.evaluateOn(evaluationRoot: Any) = this.getValue(evaluationRoot)

  private val isWebRequestOrKafkaRequestScope: Boolean
    get() = RequestContextHolder.getRequestAttributes() != null
}
