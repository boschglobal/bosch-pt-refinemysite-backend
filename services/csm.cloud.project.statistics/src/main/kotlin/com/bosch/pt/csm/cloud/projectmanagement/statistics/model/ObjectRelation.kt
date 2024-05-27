/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.model

import com.bosch.pt.csm.cloud.common.api.AbstractPersistable
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
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

    // left object
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "type", column = Column(name = "CHILD_TYPE")),
        AttributeOverride(name = "identifier", column = Column(name = "CHILD_IDENTIFIER")))
    var left: ObjectIdentifier,

    // right object
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "type", column = Column(name = "PARENT_TYPE")),
        AttributeOverride(name = "identifier", column = Column(name = "PARENT_IDENTIFIER")))
    var right: ObjectIdentifier
) : AbstractPersistable<Long>()
