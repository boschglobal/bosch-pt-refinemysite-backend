/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.consents.facade.rest

import com.bosch.pt.csm.cloud.common.extensions.toEpochMilli
import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.request.BatchRequestResource
import com.bosch.pt.csm.cloud.usermanagement.common.facade.rest.RegisteredUserPrincipal
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.DelayConsentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.api.GiveConsentCommand
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.handler.DelayConsentCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.handler.GiveConsentCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.consents.consents.command.snapshotstore.ConsentsUserSnapshotStore
import com.bosch.pt.csm.cloud.usermanagement.consents.document.Client
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersionId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.query.DocumentsQueryService
import com.bosch.pt.csm.cloud.usermanagement.user.user.shared.model.User
import java.time.LocalDateTime
import jakarta.validation.Valid
import kotlin.time.Duration.Companion.hours
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ApiVersion(from = 3)
@RestController
@RequestMapping(path = ["/users/current"])
class CurrentUserDocumentsController(
    private val documentsQueryService: DocumentsQueryService,
    private val consentsUserSnapshotStore: ConsentsUserSnapshotStore,
    private val delayConsentCommandHandler: DelayConsentCommandHandler,
    private val giveConsentCommandHandler: GiveConsentCommandHandler
) {
  private val delayConsentDuration = 16.hours

  @GetMapping("/documents")
  fun getCurrentUsersDocuments(
      @RegisteredUserPrincipal currentUser: User,
      @RequestParam client: String
  ): ResponseEntity<DocumentListResource> {
    val consentsUser = consentsUserSnapshotStore.findOrCreateSnapshot(currentUser.identifier)
    val now = LocalDateTime.now()
    val askAgainAt = consentsUser.delayedAt.plusNanos(delayConsentDuration.inWholeNanoseconds)
    val delayed = (askAgainAt.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)

    return ResponseEntity.ok(
        DocumentListResource(
            delayed = delayed,
            items =
                documentsQueryService
                    .findDocuments(
                        currentUser.country, currentUser.locale, Client.valueOf(client.uppercase()))
                    .map { document ->
                      val latestVersionIdentifier = document.latestVersion().identifier
                      DocumentResource(
                          latestVersionIdentifier.toString(),
                          document.title,
                          document.url.toString(),
                          consentsUser.hasGivenConsent(latestVersionIdentifier),
                          document.documentType.toString())
                    }))
  }

  @PostMapping("/consents")
  fun giveConsents(
      @RequestBody @Valid body: BatchRequestResource,
      @RegisteredUserPrincipal currentUser: User
  ): ResponseEntity<Void> {
    giveConsentCommandHandler.handle(
        GiveConsentCommand(
            currentUser.identifier, body.ids.map { DocumentVersionId(it.toString()) }))

    return ResponseEntity.ok().build()
  }

  @PostMapping("/documents/delay-consent")
  fun delayConsent(@RegisteredUserPrincipal currentUser: User): ResponseEntity<Void> {
    delayConsentCommandHandler.handle(DelayConsentCommand(currentUser.identifier))

    return ResponseEntity.ok().build()
  }
}

data class DocumentListResource(
    val delayed: Long = 0,
    val items: List<DocumentResource> = emptyList()
)

data class DocumentResource(
    val id: String,
    val displayName: String,
    val url: String,
    val consented: Boolean,
    val type: String
)
