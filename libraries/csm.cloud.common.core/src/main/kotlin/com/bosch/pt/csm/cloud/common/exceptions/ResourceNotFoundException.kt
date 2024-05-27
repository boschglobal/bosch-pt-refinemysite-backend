/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.exceptions

@Deprecated(
    "Use AggregateNotFoundException, which is less technology-specific.",
    replaceWith = ReplaceWith("AggregateNotFoundException"))
class ResourceNotFoundException
@JvmOverloads
constructor(val messageKey: String, cause: Throwable? = null) : RuntimeException(messageKey, cause)
