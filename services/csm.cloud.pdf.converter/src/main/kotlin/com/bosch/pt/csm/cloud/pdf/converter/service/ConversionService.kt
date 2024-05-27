/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf.converter.service

import com.bosch.pt.csm.cloud.pdf.common.exception.CallbackHttpStatusException
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Route
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.Media
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import java.time.Duration
import java.util.Optional
import java.util.function.Function
import kotlin.math.ceil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service

@Service
class ConversionService(
    @Value("\${custom.html-load-and-render-timeout}") private val connectionTimout: Duration
) {

  fun convert(
      browser: Browser,
      uri: String,
      locale: String,
      token: String,
      data: String
  ): ByteArray {
    val tracer = GlobalTracer.get()
    val traceId = tracer.activeSpan().context().toTraceId()
    val parentSpanId = tracer.activeSpan().context().toSpanId()

    val context =
        browser.newContext(Browser.NewContextOptions().apply { javaScriptEnabled = false })

    LOGGER.trace("Converting HTML page $uri to PDF ...")

    try {
      val page = context.newPage()
      page.waitForLoadState(LoadState.DOMCONTENTLOADED)
      page.route("**/*") {
        it.resume(
            Route.ResumeOptions().apply {
              setMethod("POST")
              setPostData(data)
            })
      }

      val response =
          trace(tracer, "goto page and render html") {
            page.setExtraHTTPHeaders(
                mapOf(
                    HttpHeaders.ACCEPT to "application/xhtml+xml",
                    HttpHeaders.ACCEPT_LANGUAGE to locale,
                    HttpHeaders.AUTHORIZATION to token,
                    HttpHeaders.CONTENT_TYPE to "application/json",
                    "x-datadog-trace-id" to traceId,
                    "x-datadog-parent-id" to parentSpanId,
                    "x-datadog-sampling-priority" to "1",
                    "x-datadog-origin" to it.context().toSpanId()))

            LOGGER.trace("Navigating to HTML page in chrome ...")
            page.navigate(
                uri,
                Page.NavigateOptions().apply { timeout = connectionTimout.toMillis().toDouble() })
          }

      if (response.status() >= 400) {
        // Pass through any HTTP error status to the caller
        throw CallbackHttpStatusException(response.status())
      }

      // Get page dimensions
      val canvas = page.querySelector("#canvas")
      val boundingBox = canvas.boundingBox()
      val safetyPageMargin = 5

      // Create PDF
      page.emulateMedia(Page.EmulateMediaOptions().apply { media = Optional.of(Media.SCREEN) })
      return trace(tracer, "convert html page to pdf") {
        LOGGER.trace("Save page as PDF in chrome ...")
        page.pdf(
                Page.PdfOptions().apply {
                  printBackground = true
                  width = "${ceil(boundingBox.width)}px"
                  height = "${ceil(boundingBox.height) + safetyPageMargin}px"
                })
            .also { LOGGER.trace("Returning PDF ...") }
      }
    } finally {
      context.close()
    }
  }

  private fun <T> trace(tracer: Tracer, operationName: String, function: Function<Span, T>): T {
    val span = tracer.buildSpan(operationName).start()
    try {
      return function.apply(span)
    } finally {
      span.finish()
    }
  }

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(ConversionService::class.java)
  }
}
