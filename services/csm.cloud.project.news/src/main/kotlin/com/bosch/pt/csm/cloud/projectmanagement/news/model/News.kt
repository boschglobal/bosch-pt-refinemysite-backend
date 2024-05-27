/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.model

import java.time.Instant
import java.util.UUID
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    indexes =
        [
            Index(
                name = "IX_News_ContIdentContTyp", columnList = "context_identifier,context_type"),
            Index(
                name = "IX_News_UsrIdRooIdentRooTyp",
                columnList = "user_identifier,root_identifier,root_type")],
    uniqueConstraints =
        [
            UniqueConstraint(
                name = "UK_News_UsrIdConIdentContTyp",
                columnNames = ["user_identifier", "context_identifier", "context_type"])])
class News(
    // rootObject
    @AttributeOverride(name = "type", column = Column(name = "ROOT_TYPE"))
    @AttributeOverride(name = "identifier", column = Column(name = "ROOT_IDENTIFIER"))
    var rootObject: ObjectIdentifier,

    // parentObject
    @AttributeOverride(name = "type", column = Column(name = "PARENT_TYPE"))
    @AttributeOverride(name = "identifier", column = Column(name = "PARENT_IDENTIFIER"))
    var parentObject: ObjectIdentifier,

    // contextObject
    contextObject: ObjectIdentifier,

    // userIdentifier
    userIdentifier: UUID?,

    // createdDate
    var createdDate: Instant,

    // lastModifiedDate
    var lastModifiedDate: Instant
) : NewsIdentifier(userIdentifier, contextObject)
