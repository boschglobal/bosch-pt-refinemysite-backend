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
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum.US
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.EULA
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType.TERMS_AND_CONDITIONS
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import java.util.Locale
import java.util.Locale.ENGLISH
import java.util.stream.Collectors.groupingBy
import org.slf4j.Logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentsQueryService(
    private val documentSnapshotStore: DocumentSnapshotStore,
    private val logger: Logger
) {
  companion object {
    private val FALLBACK_COUNTRY = DE
    private val FALLBACK_LOCALE = ENGLISH
  }

  @Cacheable("documents")
  @Transactional(readOnly = true)
  fun findDocuments(
      country: IsoCountryCodeEnum?,
      locale: Locale?,
      client: Client? = null
  ): List<DocumentSnapshot> {
    @Suppress("NAME_SHADOWING") val country = country ?: FALLBACK_COUNTRY
    @Suppress("NAME_SHADOWING") val locale = (locale ?: FALLBACK_LOCALE).onlyLanguage()
    val documents =
        if (client != null) findDocumentsInternal(country, locale, { it.mostSpecificFor(client) })
        else findDocumentsInternal(country, locale, { it })
    if (documents.none { it.documentType == TERMS_AND_CONDITIONS })
        logger.warn(
            "No document or fallback of type $TERMS_AND_CONDITIONS found for: " +
                "country: $country, locale: $locale, client: $client")
    if (country == US && documents.none { it.documentType == EULA })
        logger.warn(
            "No document or fallback of type $EULA found for: " +
                "country: $country, locale: $locale, client: $client")
    return documents
  }

  private fun Locale.onlyLanguage() = Locale(language)

  private fun findDocumentsInternal(
      country: IsoCountryCodeEnum,
      locale: Locale,
      clientQuery: (List<DocumentSnapshot>) -> List<DocumentSnapshot>
  ): List<DocumentSnapshot> {
    val documentsByTypeAndCountryAndLocaleAndClient = fetchDocuments(country, locale)

    val termsDocument =
        documentsByTypeAndCountryAndLocaleAndClient[TERMS_AND_CONDITIONS]?.queryWithFallback(
            country, FALLBACK_COUNTRY) {
              it.queryWithFallback(locale, FALLBACK_LOCALE, clientQuery)
            }

    val eulaDocument =
        documentsByTypeAndCountryAndLocaleAndClient[EULA]
            ?.get(country) // no fallback country for EULAs
            ?.queryWithFallback(locale, FALLBACK_LOCALE, clientQuery)

    return listOfNotNull(termsDocument, eulaDocument).flatten()
  }

  private fun List<DocumentSnapshot>.mostSpecificFor(client: Client): List<DocumentSnapshot> =
      listOfNotNull(filter { client in it.clientSet }.maxByOrNull { it.clientSet.specificity })

  private fun fetchDocuments(
      country: IsoCountryCodeEnum,
      locale: Locale
  ): Map<DocumentType, Map<IsoCountryCodeEnum, Map<Locale, List<DocumentSnapshot>>>> =
      documentSnapshotStore
          .findByCountryInOrLocaleIn(
              listOf(country, FALLBACK_COUNTRY), listOf(locale, FALLBACK_LOCALE))
          .stream()
          .collect(
              groupingBy({ it.documentType }, groupingBy({ it.country }, groupingBy { it.locale })))

  private fun <K, V> Map<K, V>.queryWithFallback(
      key: K,
      fallbackKey: K,
      query: (V) -> List<DocumentSnapshot>
  ): List<DocumentSnapshot> {
    val queryResult = get(key)?.let(query)
    return if (queryResult.isNullOrEmpty()) {
      get(fallbackKey)?.let(query) ?: emptyList()
    } else {
      queryResult
    }
  }
}
