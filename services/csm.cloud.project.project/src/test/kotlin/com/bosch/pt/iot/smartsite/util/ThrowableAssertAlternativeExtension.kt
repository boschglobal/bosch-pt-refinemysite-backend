/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.util

import org.assertj.core.api.ThrowableAssertAlternative

fun ThrowableAssertAlternative<*>.withMessageKey(messageKey: String) =
    this.extracting("messageKey").isEqualTo(messageKey)
