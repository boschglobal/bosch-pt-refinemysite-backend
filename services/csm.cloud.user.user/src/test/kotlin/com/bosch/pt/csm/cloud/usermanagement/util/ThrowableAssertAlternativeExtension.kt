/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.util

import com.bosch.pt.csm.cloud.common.LibraryCandidate
import org.assertj.core.api.ThrowableAssertAlternative

@LibraryCandidate("common.core")
fun ThrowableAssertAlternative<*>.withMessageKey(messageKey: String) =
    this.extracting("messageKey").isEqualTo(messageKey)
