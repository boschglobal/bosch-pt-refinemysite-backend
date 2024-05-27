/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.query

import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.DE
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.ES
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet.ALL
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet.MOBILE
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet.WEB
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.EULA
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.TERMS_AND_CONDITIONS
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import java.net.URL
import java.time.LocalDateTime
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.Locale.GERMAN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger

private val SPANISH = Locale("es")

@ExtendWith(MockKExtension::class)
class DocumentsQueryServiceTest {

  @MockK private lateinit var documentSnapshotStore: DocumentSnapshotStore
  @Suppress("UnusedPrivateMember") @RelaxedMockK private lateinit var logger: Logger

  @InjectMockKs private lateinit var documentsQueryService: DocumentsQueryService

  private val fallbackCountry = DE
  private val fallbackLocale = ENGLISH
  private val fallbackClient = ALL

  @Test
  fun `findDocuments returns documents for requested country, locale and client`() {
    val termsDocument = createDocumentFor(TERMS_AND_CONDITIONS, US, SPANISH, WEB)
    val eulaDocument = createDocumentFor(EULA, US, SPANISH, WEB)
    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(termsDocument, eulaDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnlyOnce(termsDocument, eulaDocument)
  }

  @Test
  fun `findDocuments returns no documents if none exist`() {
    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns emptyList()

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).isEmpty()
  }

  @Test
  fun `findDocuments returns no documents if no relevant exists`() {
    val documentWithIrrelevantLocale = createDocumentFor(TERMS_AND_CONDITIONS, US, GERMAN, WEB)
    val documentWithIrrelevantCountry = createDocumentFor(TERMS_AND_CONDITIONS, ES, SPANISH, WEB)
    val documentWithIrrelevantClient = createDocumentFor(TERMS_AND_CONDITIONS, US, SPANISH, MOBILE)
    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(
            documentWithIrrelevantLocale,
            documentWithIrrelevantCountry,
            documentWithIrrelevantClient,
        )

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).isEmpty()
  }

  @Test
  fun `language fallback takes precedence over country fallback`() {
    val languageFallbackDocument = createDocumentFor(TERMS_AND_CONDITIONS, US, fallbackLocale, WEB)
    val countryFallbackDocument =
        createDocumentFor(TERMS_AND_CONDITIONS, fallbackCountry, SPANISH, WEB)

    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(languageFallbackDocument, countryFallbackDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnlyOnce(languageFallbackDocument)
  }

  @Test
  fun `client fallback takes precedence over language fallback`() {
    val clientFallbackDocument =
        createDocumentFor(TERMS_AND_CONDITIONS, US, SPANISH, fallbackClient)
    val languageFallbackDocument = createDocumentFor(TERMS_AND_CONDITIONS, US, fallbackLocale, WEB)

    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(clientFallbackDocument, languageFallbackDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnlyOnce(clientFallbackDocument)
  }

  @Test
  fun `findDocuments returns fallback for client for specified country and locale`() {
    val termsFallbackDocument = createDocumentFor(TERMS_AND_CONDITIONS, US, SPANISH, fallbackClient)
    val eulaFallbackDocument = createDocumentFor(EULA, US, SPANISH, fallbackClient)

    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(termsFallbackDocument, eulaFallbackDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnlyOnce(termsFallbackDocument, eulaFallbackDocument)
  }

  @Test
  fun `findDocuments returns fallback for locale for specified country and client`() {
    val termsFallbackDocument = createDocumentFor(TERMS_AND_CONDITIONS, US, fallbackLocale, WEB)
    val eulaFallbackDocument = createDocumentFor(EULA, US, fallbackLocale, WEB)

    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(termsFallbackDocument, eulaFallbackDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnlyOnce(termsFallbackDocument, eulaFallbackDocument)
  }

  @Test
  fun `findDocuments returns fallback terms and conditions for country for specified locale and client`() {
    val termsFallbackDocument =
        createDocumentFor(TERMS_AND_CONDITIONS, fallbackCountry, SPANISH, WEB)

    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(termsFallbackDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnlyOnce(termsFallbackDocument)
  }

  @Test
  fun `findDocuments does not return fallback EULA for country`() {
    val eulaFallbackDocument = createDocumentFor(EULA, fallbackCountry, SPANISH, WEB)

    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(eulaFallbackDocument)

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).isEmpty()
  }

  @Test
  fun `returns fallback for locale even if doc for specified locale is available but for other client`() {
    val fallbackDocument = createDocumentFor(EULA, US, ENGLISH, WEB)
    every { documentSnapshotStore.findByCountryInOrLocaleIn(any(), any()) } returns
        listOf(
            createDocumentFor(EULA, US, SPANISH, MOBILE),
            fallbackDocument,
        )

    val documents = documentsQueryService.findDocuments(US, SPANISH, Client.WEB)

    assertThat(documents).containsOnly(fallbackDocument)
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
