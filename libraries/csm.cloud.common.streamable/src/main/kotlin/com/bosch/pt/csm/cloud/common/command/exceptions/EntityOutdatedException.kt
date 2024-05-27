/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.command.exceptions

/** Error if entity that should be updated is outdated. */
class EntityOutdatedException
@JvmOverloads
constructor(val messageKey: String, cause: Throwable? = null) : RuntimeException(cause)
