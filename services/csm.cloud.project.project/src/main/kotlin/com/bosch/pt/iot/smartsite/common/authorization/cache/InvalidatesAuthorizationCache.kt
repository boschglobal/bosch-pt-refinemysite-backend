/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization.cache

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Used in combination with [AuthorizationCacheKey] to invalidate authorization cache entries after
 * the annotated method returns. The [AuthorizationCacheKey] needs to be used on the method
 * parameter that holds the cache key to be invalidated.
 *
 * Typically, this annotation is used on methods that mutate a potentially cached entry. For
 * example, a Task might be cached; after updating the Task, the cached entry is updated and should
 * be invalidated.
 */
@Target(FUNCTION) @Retention(RUNTIME) annotation class InvalidatesAuthorizationCache
