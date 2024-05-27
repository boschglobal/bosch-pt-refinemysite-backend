/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf.application.pdf

import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Playwright
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory

class PlaywrightFactory(
    initialInstances: Int,
    private val queue: ConcurrentLinkedQueue<PlaywrightInstance> = ConcurrentLinkedQueue()
) {

  companion object {
    private val LOGGER = LoggerFactory.getLogger(PlaywrightFactory::class.java)
  }

  init {
    for (idx in 0 until initialInstances) {
      LOGGER.debug("Instantiate browser instance #$idx")
      queue.add(newInstance())
    }
  }

  @Suppress("SwallowedException")
  fun close() {
    for (instance in queue) {
      try {
        instance.close()
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        // do nothing
      }
    }
  }

  fun getInstance(): PlaywrightInstance {
    synchronized(this) {
      Executors.newSingleThreadExecutor().submit { queue.add(newInstance()) }
      return queue.remove()
    }
  }

  private fun newInstance(): PlaywrightInstance {
    val playwright = Playwright.create()
    val browser =
        playwright
            .chromium()
            .launch(LaunchOptions().setArgs(listOf("--disable-field-trial-config")))
    return PlaywrightInstance(playwright, browser)
  }
}
