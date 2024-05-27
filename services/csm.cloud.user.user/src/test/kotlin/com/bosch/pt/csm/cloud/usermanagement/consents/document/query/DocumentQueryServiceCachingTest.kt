/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.query

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.application.config.CacheConfig
import com.bosch.pt.csm.cloud.usermanagement.application.config.LoggerConfiguration
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import java.net.URL
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ImportAutoConfiguration(CacheAutoConfiguration::class)
@Import(DocumentsQueryService::class, CacheConfig::class, LoggerConfiguration::class)
@TestPropertySource(
    properties = ["spring.cache.type=jcache", "spring.cache.jcache.config=classpath:ehcache.xml"])
class DocumentQueryServiceCachingTest {

  @MockkBean private lateinit var documentSnapshotStore: DocumentSnapshotStore

  @Test
  fun `caches document queries`(@Autowired documentsQueryService: DocumentsQueryService) {
    val spanish = Locale("es")
    val termsDocument =
        createDocumentFor(
            DocumentType.TERMS_AND_CONDITIONS, IsoCountryCodeEnum.US, spanish, ClientSet.WEB)
    val eulaDocument =
        createDocumentFor(DocumentType.EULA, IsoCountryCodeEnum.US, spanish, ClientSet.WEB)
    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(termsDocument, eulaDocument)

    repeat(2) { documentsQueryService.findDocuments(IsoCountryCodeEnum.US, spanish, Client.WEB) }

    verify(exactly = 1) { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) }
  }

  private fun createDocumentFor(
      type: DocumentType,
      country: IsoCountryCodeEnum,
      locale: Locale,
      clientSet: ClientSet,
  ) =
      DocumentSnapshot(
          "",
          URL("https://example.com"),
          type,
          country,
          locale,
          clientSet,
          listOf(DocumentVersion(DocumentVersionId(), LocalDateTime.now())),
          DocumentId(),
          0)
}
