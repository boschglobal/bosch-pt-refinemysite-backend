/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.ErrorResource
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.ChangeDocumentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.CreateDocumentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.document.api.IncrementDocumentVersionCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.handler.ChangeDocumentCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.handler.CreateDocumentCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.handler.IncrementDocumentVersionCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshot
import com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore.DocumentSnapshotStore
import datadog.trace.api.GlobalTracer
import java.net.URL
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID.randomUUID
import kotlin.text.Charsets.UTF_8
import org.apache.commons.lang3.LocaleUtils
import org.slf4j.Logger
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ApiVersion(from = 1, to = 1)
@RestController
@RequestMapping(path = ["/documents"])
@AdminAuthorization
class AdminDocumentsController(
    private val documentSnapshotStore: DocumentSnapshotStore,
    private val createDocumentCommandHandler: CreateDocumentCommandHandler,
    private val incrementDocumentVersionCommandHandler: IncrementDocumentVersionCommandHandler,
    private val changeDocumentCommandHandler: ChangeDocumentCommandHandler,
    private val logger: Logger,
) {

  @PostMapping
  fun createDocument(@RequestBody body: DocumentAdminCreateResource): ResponseEntity<Any> =
      try {
        val documentId = createDocumentCommandHandler.handle(body.toCommand())

        val createdDocument = documentSnapshotStore.findOrFail(documentId)

        ResponseEntity.ok(createdDocument.toResource())
      } catch (e: DataIntegrityViolationException) {
        logger.debug(e.stackTraceToString())
        ResponseEntity.badRequest()
            .body(ErrorResource(e.cause?.cause?.message ?: throw e, GlobalTracer.get().traceId))
      } catch (e: IllegalArgumentException) {
        logger.debug(e.stackTraceToString())
        ResponseEntity.badRequest().body(ErrorResource(e.message ?: "", GlobalTracer.get().traceId))
      }

  @PostMapping("/{documentId}/versions")
  fun incrementVersion(
      @PathVariable documentId: DocumentId,
      @RequestBody body: DocumentVersionIncrementAdminResource
  ): ResponseEntity<DocumentAdminResource> {
    incrementDocumentVersionCommandHandler.handle(body.toCommand(documentId))

    val documentWithIncrementedVersion = documentSnapshotStore.findOrFail(documentId)

    return ResponseEntity.ok(documentWithIncrementedVersion.toResource())
  }

  @GetMapping
  fun getAllDocuments(): ResponseEntity<DocumentAdminListResource> =
      ResponseEntity.ok(
          DocumentAdminListResource(
              items = documentSnapshotStore.findAll().map { it.toResource() }))

  @GetMapping("/{documentId}")
  fun getDocumentById(@PathVariable documentId: DocumentId): ResponseEntity<DocumentAdminResource> {
    val document = documentSnapshotStore.findOrFail(documentId)
    return ResponseEntity.ok(document.toResource())
  }

  @PutMapping("/{documentId}")
  fun updateDocument(
      @PathVariable documentId: DocumentId,
      @RequestBody body: DocumentAdminUpdateResource,
  ): ResponseEntity<DocumentAdminResource> {
    changeDocumentCommandHandler.handle(body.toCommand(documentId))

    val updatedDocument = documentSnapshotStore.findOrFail(documentId)

    return ResponseEntity.ok(updatedDocument.toResource())
  }
}

data class DocumentAdminCreateResource(
    val type: String,
    val country: String,
    val locale: String,
    val client: String,
    val displayName: String,
    val url: String,
    val lastChanged: Instant
)

data class DocumentAdminUpdateResource(val displayName: String?, val url: String?)

data class DocumentVersionAdminResource(val identifier: String, val lastChanged: Instant)

data class DocumentAdminResource(
    val identifier: String,
    val type: String,
    val country: String,
    val locale: String,
    val client: String,
    val displayName: String,
    val url: String,
    val versions: List<DocumentVersionAdminResource>
)

data class DocumentAdminListResource(val items: List<DocumentAdminResource>)

fun DocumentSnapshot.toResource() =
    DocumentAdminResource(
        identifier.toString(),
        documentType.toString(),
        country.toString(),
        locale.toString(),
        clientSet.toString(),
        title,
        url.toString(),
        versions.map { it.toResource() })

fun DocumentVersion.toResource() =
    DocumentVersionAdminResource(identifier.toString(), lastChanged.toInstant(UTC))

fun DocumentAdminCreateResource.toCommand(): CreateDocumentCommand {
  val country =
      IsoCountryCodeEnum.fromCountryCode(country)
          ?: throw IllegalArgumentException("Country $country not found")
  val locale = LocaleUtils.toLocale(locale)

  return CreateDocumentCommand(
      DocumentId(randomUUID()),
      DocumentType.valueOf(type),
      country,
      locale,
      ClientSet.valueOf(client),
      displayName,
      URL(URLDecoder.decode(url, UTF_8)),
      DocumentVersion(DocumentVersionId(), LocalDateTime.ofInstant(lastChanged, UTC)))
}

data class DocumentVersionIncrementAdminResource(val lastChanged: Instant)

fun DocumentVersionIncrementAdminResource.toCommand(
    documentId: DocumentId,
    documentVersionIdentifier: DocumentVersionId = DocumentVersionId()
) =
    IncrementDocumentVersionCommand(
        documentId,
        DocumentVersion(documentVersionIdentifier, LocalDateTime.ofInstant(lastChanged, UTC)))

private fun DocumentAdminUpdateResource.toCommand(documentId: DocumentId) =
    ChangeDocumentCommand(documentId, displayName, url?.let { URL(URLDecoder.decode(url, UTF_8)) })
