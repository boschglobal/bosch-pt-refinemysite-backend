/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.exceptions

import java.lang.RuntimeException

class BlockOperationsException(val messageKey: String) : RuntimeException()