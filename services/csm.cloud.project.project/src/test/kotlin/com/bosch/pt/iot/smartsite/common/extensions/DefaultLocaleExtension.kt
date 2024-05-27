/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.extensions

import java.util.Locale
import java.util.Locale.UK
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Sets the default Locale to english for all tests, so system locale is never used */
class DefaultLocaleExtension : BeforeEachCallback {
  override fun beforeEach(context: ExtensionContext?) {
    Locale.setDefault(UK)
  }
}
