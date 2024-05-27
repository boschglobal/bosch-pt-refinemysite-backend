/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.user.model.Document
import com.bosch.pt.iot.smartsite.dataimport.user.rest.ConsentsRestClient
import com.bosch.pt.iot.smartsite.dataimport.user.rest.ConsentsRestClient.CreateDocumentResource
import org.springframework.stereotype.Service

@Service
class DocumentsImportService(
    private val authenticationService: AuthenticationService,
    private val consentsRestClient: ConsentsRestClient
) : ImportService<Document> {

  override fun importData(data: Document) {
    authenticationService.selectAdmin()
    call { consentsRestClient.create(data.toCreateResource()) }
  }

  private fun Document.toCreateResource() =
      CreateDocumentResource(
          type, country, locale, client, displayName, url, versions.first().lastChanged)
}
