/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.authorization.cache

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Target(VALUE_PARAMETER)
@Retention(RUNTIME)
annotation class AuthorizationCacheKey(
    /**
     * a Spring expression language (SpEl) expression pointing to the field that holds the cache
     * key, e.g. "identifier". The SpEl expression is required if the annotated parameter is a DTO
     * or a collection of DTOs. Otherwise, if the annotated parameter is a UUID or a collection of
     * UUIDs, this expression should remain empty.
     */
    val field: String = ""
)
