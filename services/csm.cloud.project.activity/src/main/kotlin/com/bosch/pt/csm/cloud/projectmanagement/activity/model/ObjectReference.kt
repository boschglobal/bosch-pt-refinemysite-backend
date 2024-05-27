/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.model

import java.util.UUID
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

// This is a marker interface for classes that can be used as object reference.
interface ObjectReference

/**
 * This class represents an unresolved reference to another aggregate (i.e. a user). The
 * corresponding display names of the referenced aggregate will be determined during retrieval of
 * the activity.
 */
@Document
@TypeAlias("UnresolvedObjectReference")
data class UnresolvedObjectReference(
    val type: String,
    val identifier: UUID,
    val contextRootIdentifier: UUID
) : ObjectReference, Value

/**
 * This class represents a resolved reference to another aggregate (i.e. a daycard). The
 * corresponding display names of the referenced aggregates will be the store value display name.
 */
@Document
@TypeAlias("ResolvedObjectReference")
data class ResolvedObjectReference(
    val type: String,
    val identifier: UUID,
    val displayName: String
) : ObjectReference
