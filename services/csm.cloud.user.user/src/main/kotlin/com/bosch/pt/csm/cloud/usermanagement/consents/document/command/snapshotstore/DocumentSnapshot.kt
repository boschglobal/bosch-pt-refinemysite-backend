/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.handler.CommandHandler
import com.bosch.pt.csm.cloud.common.command.snapshotstore.VersionedSnapshot
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentVersion
import com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model.Document
import java.net.URL
import java.util.Locale

data class DocumentSnapshot(
    val title: String,
    val url: URL,
    val documentType: DocumentType,
    val country: IsoCountryCodeEnum,
    val locale: Locale,
    val clientSet: ClientSet,
    val versions: List<DocumentVersion>,
    override val identifier: DocumentId,
    override val version: Long,
) : VersionedSnapshot {
  fun latestVersion(): DocumentVersion =
      this.versions.maxByOrNull { it.lastChanged }
          ?: error("Found a document without a version! $this")

  fun toCommandHandler() = CommandHandler.of(this)
}

fun Document.asValueObject() =
    DocumentSnapshot(
        title,
        url,
        documentType,
        country,
        locale,
        client,
        versions.map { DocumentVersion(it.identifier, it.lastChanged) },
        identifier,
        version)
