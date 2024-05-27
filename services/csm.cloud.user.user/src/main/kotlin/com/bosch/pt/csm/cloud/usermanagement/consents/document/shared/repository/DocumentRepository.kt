/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.repository

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.Document
import java.util.Locale
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long> {
  fun existsByClientAndDocumentTypeAndCountryAndLocale(
      clientSet: ClientSet,
      documentType: DocumentType,
      country: IsoCountryCodeEnum,
      locale: Locale
  ): Boolean
  fun findByCountryInOrLocaleIn(
      countries: List<IsoCountryCodeEnum>,
      locales: List<Locale>
  ): List<Document>
  fun findByIdentifier(identifier: DocumentId): Document?
}
