/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.versioning

import java.lang.RuntimeException

class UnsupportedApiVersionException(val minVersion: Int, val maxVersion: Int) : RuntimeException()
