/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotStoreJpa
import com.bosch.pt.csm.cloud.common.exceptions.AggregateNotFoundException
import com.bosch.pt.csm.cloud.common.extensions.toLocalDateTimeByMillis
import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.common.model.key.AggregateEventMessageKey
import com.bosch.pt.csm.cloud.usermanagement.common.translation.Key.DOCUMENT_VALIDATION_ERROR_NOT_FOUND
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.Document
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.repository.DocumentRepository
import com.bosch.pt.csm.cloud.usermanagement.consents.eventstore.ConsentsContextSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentChangedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentCreatedEventAvro
import com.bosch.pt.csm.cloud.usermanagement.consents.messages.DocumentVersionIncrementedEventAvro
import java.net.URL
import java.util.Locale
import java.util.UUID
import org.apache.avro.specific.SpecificRecordBase
import org.springframework.stereotype.Component

@Component
class DocumentSnapshotStore(private val repository: DocumentRepository) :
    AbstractSnapshotStoreJpa<SpecificRecordBase, DocumentSnapshot, Document, DocumentId>(),
    ConsentsContextSnapshotStore {
  override fun findInternal(identifier: UUID) = repository.findByIdentifier(DocumentId(identifier))

  override fun handlesMessage(key: AggregateEventMessageKey, message: SpecificRecordBase) =
      message is DocumentCreatedEventAvro ||
          message is DocumentVersionIncrementedEventAvro ||
          message is DocumentChangedEventAvro

  override fun isDeletedEvent(message: SpecificRecordBase) = false

  override fun updateInternal(event: SpecificRecordBase, currentSnapshot: Document?): Long {
    when (event) {
      is DocumentCreatedEventAvro -> {
        val createdSnapshot =
            Document(
                    event.title,
                    URL(event.url),
                    ClientSet.valueOf(event.client),
                    DocumentType.valueOf(event.type),
                    IsoCountryCodeEnum.valueOf(event.country.toString()),
                    Locale(event.locale),
                    mutableListOf(
                        DocumentVersion(
                            DocumentVersionId(event.initialVersion.identifier),
                            event.initialVersion.lastChanged.toLocalDateTimeByMillis())))
                .apply {
                  identifier = DocumentId(event.aggregateIdentifier.identifier.toUUID())
                  version = event.aggregateIdentifier.version
                  setCreatedBy(UserId(event.auditingInformation.user.toUUID()))
                  setCreatedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
                  setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
                  setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
                }

        return repository.saveAndFlush(createdSnapshot).version
      }
      is DocumentVersionIncrementedEventAvro -> {
        checkNotNull(currentSnapshot)

        currentSnapshot.apply {
          versions.add(
              DocumentVersion(
                  DocumentVersionId(event.version.identifier),
                  event.version.lastChanged.toLocalDateTimeByMillis()))
          version = event.aggregateIdentifier.version
          setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
          setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
        }

        return repository.saveAndFlush(currentSnapshot).version
      }
      is DocumentChangedEventAvro -> {
        checkNotNull(currentSnapshot)

        currentSnapshot.apply {
          if (event.title != null) {
            title = event.title
          }
          if (event.url != null) {
            url = URL(event.url)
          }
          version = event.aggregateIdentifier.version
          setLastModifiedBy(UserId(event.auditingInformation.user.toUUID()))
          setLastModifiedDate(event.auditingInformation.date.toLocalDateTimeByMillis())
        }

        return repository.saveAndFlush(currentSnapshot).version
      }
      else -> error("Snapshot can't handle event ${event::class}. This should not happen.")
    }
  }

  override fun findOrFail(identifier: DocumentId): DocumentSnapshot {
    return repository.findByIdentifier(identifier)?.asValueObject()
        ?: throw AggregateNotFoundException(
            DOCUMENT_VALIDATION_ERROR_NOT_FOUND, identifier.toString())
  }

  fun findByCountryInOrLocaleIn(
      countries: List<IsoCountryCodeEnum>,
      locales: List<Locale>
  ): List<DocumentSnapshot> =
      repository.findByCountryInOrLocaleIn(countries, locales).map { it.asValueObject() }

  fun findAll(): List<DocumentSnapshot> = repository.findAll().map { it.asValueObject() }

  fun existsByClientAndDocumentTypeAndCountryAndLocale(
      clientSet: ClientSet,
      documentType: DocumentType,
      country: IsoCountryCodeEnum,
      locale: Locale
  ) =
      repository.existsByClientAndDocumentTypeAndCountryAndLocale(
          clientSet, documentType, country, locale)
}
