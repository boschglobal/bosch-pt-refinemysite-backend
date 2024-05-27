/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.pdf.converter.facade.rest

import com.bosch.pt.csm.cloud.pdf.application.pdf.PlaywrightFactory
import com.bosch.pt.csm.cloud.pdf.common.exception.CallbackHttpStatusException
import com.bosch.pt.csm.cloud.pdf.converter.service.ConversionService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Suppress("ThrowsCount")
class PdfConverterController(
    private val conversionService: ConversionService,
    private val playwrightFactory: PlaywrightFactory
) {

  @PostMapping(
      "/convert",
      consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
      produces = [MediaType.APPLICATION_PDF_VALUE])
  fun convert(@RequestBody data: MultiValueMap<String, String>): ResponseEntity<ByteArray> {
    val dataUri = data.getFirst("uri") ?: throw IllegalArgumentException("Uri missing")
    val dataLocale = data.getFirst("locale") ?: throw IllegalArgumentException("Locale missing")
    val dataToken = data.getFirst("token") ?: throw IllegalArgumentException("Token missing")
    val dataData = data.getFirst("data") ?: throw IllegalArgumentException("Data missing")

    val playwright = playwrightFactory.getInstance()
    return try {
      ResponseEntity.ok(
          conversionService.convert(playwright.browser, dataUri, dataLocale, dataToken, dataData))
    } catch (ex: CallbackHttpStatusException) {
      ResponseEntity.status(ex.statusCode).build()
    } finally {
      playwright.close()
    }
  }
}
