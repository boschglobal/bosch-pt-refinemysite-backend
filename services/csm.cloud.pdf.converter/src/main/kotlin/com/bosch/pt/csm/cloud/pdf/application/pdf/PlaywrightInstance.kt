/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf.application.pdf

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Playwright

class PlaywrightInstance(private val playwright: Playwright, val browser: Browser) {
  fun close() {
    playwright.close()
  }
}
