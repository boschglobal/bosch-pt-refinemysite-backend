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
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

/** Removes every entry of the ExistenceCache containing the identifiers that deleted. */
@Aspect
@Component
class EvictExistenceCacheAspect(private val cache: ExistenceCache) {

  @Around(
      "@annotation(com.bosch.pt.iot.smartsite.common.repository.existencecache.EvictExistenceCache)")
  fun evictCacheOrFallBackToRepositoryCall(joinPoint: ProceedingJoinPoint): Any? {

    // This aspect should only be applied to web and kafka requests
    if (isWebRequestOrKafkaRequestScope) {
      require(joinPoint.args.isNotEmpty()) {
        "Function annotated with @${EvictExistenceCache::class.java.simpleName} that does not contain any argument."
      }

      evictCacheEntries(joinPoint.args.first())
    }

    return joinPoint.proceed()
  }

  private fun evictCacheEntries(entity: Any) {
    when (entity) {
      is Collection<*> -> entity.forEach { it?.let { evictCacheEntries(it) } }
      is AbstractSnapshotEntity<*, *> -> cache.remove(entity.identifier)
      is AbstractEntity<*, *> -> cache.remove(entity.identifier!!)
      is AbstractReplicatedEntity<*> -> cache.remove(entity.identifier!!)
      else ->
          throw IllegalArgumentException(
              "A method annotated with @${EvictExistenceCache::class.java.simpleName} " +
                  "must contain an supported entity as argument.")
    }
  }

  private val isWebRequestOrKafkaRequestScope: Boolean
    get() = RequestContextHolder.getRequestAttributes() != null
}
