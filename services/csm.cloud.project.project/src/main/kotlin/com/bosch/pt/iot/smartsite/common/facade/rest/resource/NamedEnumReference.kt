/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.facade.rest.resource

data class NamedEnumReference<T : Enum<*>>(val key: T, val name: String)
