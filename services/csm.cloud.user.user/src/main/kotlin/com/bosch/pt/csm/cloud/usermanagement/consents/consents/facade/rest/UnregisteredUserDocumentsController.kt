/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.query.DocumentsQueryService
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ApiVersion(from = 3)
@RestController
@RequestMapping(path = ["/users/unregistered/documents"])
class UnregisteredUserDocumentsController(
    private val documentsQueryService: DocumentsQueryService
) {

  @GetMapping
  fun getDocumentsByCountryAndLocale(
      @RequestParam country: String,
      @RequestParam locale: String,
      @RequestParam client: String
  ): ResponseEntity<UnregisteredUserDocumentListResource> {
    val docs =
        documentsQueryService.findDocuments(
            IsoCountryCodeEnum.fromCountryCode(country),
            StringUtils.parseLocale(locale),
            Client.valueOf(client.uppercase()),
        )

    return ResponseEntity.ok(
        UnregisteredUserDocumentListResource(
            docs.map { document ->
              val latestVersionIdentifier = document.latestVersion().identifier
              UnregisteredUserDocumentResource(
                  latestVersionIdentifier.toString(),
                  document.title,
                  document.url.toString(),
                  document.documentType.toString())
            }))
  }
}

data class UnregisteredUserDocumentListResource(
    val items: List<UnregisteredUserDocumentResource> = emptyList()
)

data class UnregisteredUserDocumentResource(
    val id: String,
    val displayName: String,
    val url: String,
    val type: String,
)
