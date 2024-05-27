/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.common.extensions

import java.util.Locale
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Sets the default Locale to english for all tests, so system locale is never used */
class DefaultLocaleExtension : BeforeEachCallback {
  override fun beforeEach(context: ExtensionContext?) {
    Locale.setDefault(Locale.UK)
  }
}
