/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.model.IsoCountryCodeEnum
import com.bosch.pt.csm.cloud.usermanagement.consents.document.ClientSet
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentId
import com.bosch.pt.csm.cloud.usermanagement.consents.document.DocumentType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.net.URL
import java.util.Locale

@Entity
@Table(
    name = "DOCUMENT",
    indexes =
        [
            Index(
                name = "UK_Document_Type_Country_Locale_Client",
                columnList = "documentType, country, locale, client",
                unique = true),
        ])
class Document(

    // title
    @Column(nullable = false) var title: String,

    // url
    @Column(nullable = false) var url: URL,

    // client
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
    @Enumerated(STRING)
    val client: ClientSet,

    // documentType
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
    @Enumerated(STRING)
    val documentType: DocumentType,

    // country
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32)")
    @Enumerated(STRING)
    val country: IsoCountryCodeEnum,

    // local
    @Column(nullable = false, length = 32) val locale: Locale,

    // versions
    @ElementCollection(fetch = EAGER)
    @CollectionTable(
        name = "DOCUMENT_VERSION",
        foreignKey = ForeignKey(name = "FK_DocumentVersion_Document"),
        uniqueConstraints =
            [
                UniqueConstraint(
                    name = "UK_DocumentVersion_Document_LastChanged",
                    columnNames = ["document_id", "lastChanged"]),
            ],
        indexes =
            [
                Index(
                    name = "UK_DocumentVersion_Identifier",
                    columnList = "identifier",
                    unique = true),
            ])
    val versions: MutableCollection<DocumentVersion>
) : AbstractSnapshotEntity<Long, DocumentId>() {
  override fun getDisplayName() = title
}
