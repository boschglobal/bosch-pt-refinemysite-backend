/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.news.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    indexes =
        [
            Index(
                name = "IX_ObjRel_ChildParent",
                columnList = "child_identifier,child_type,parent_type",
                unique = true)])
class ObjectRelation(
    // left
    @Embedded
    @AttributeOverride(name = "type", column = Column(name = "CHILD_TYPE"))
    @AttributeOverride(name = "identifier", column = Column(name = "CHILD_IDENTIFIER"))
    var left: ObjectIdentifier,

    // right
    @Embedded
    @AttributeOverride(name = "type", column = Column(name = "PARENT_TYPE"))
    @AttributeOverride(name = "identifier", column = Column(name = "PARENT_IDENTIFIER"))
    var right: ObjectIdentifier
) : AbstractPersistable<Long>()
