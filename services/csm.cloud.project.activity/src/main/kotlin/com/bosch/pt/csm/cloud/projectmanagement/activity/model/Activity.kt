/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.model

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.ACTIVITY
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = ACTIVITY)
@TypeAlias("Activity")
data class Activity(
    @Id val aggregateIdentifier: AggregateIdentifier,
    val identifier: UUID,
    val event: EventInformation,
    val summary: Summary,
    val details: Details = NoDetails(),
    val context: Context,
    val attachment: Attachment? = null
)

/** This class contains the information about the even that triggered the current activity. */
@TypeAlias("EventInformation")
data class EventInformation(val name: String, val date: Instant, val user: UUID)

/**
 * This class represents the summary of an activity. The summary is described by a template
 * (referenced by a message key). The template can contain placeholders.
 */
@TypeAlias("Summary")
data class Summary(

    /**
     * the message template referenced by a message key. The template can contain placeholders in
     * the form ${someKey}, where someKey needs to be a key either in the references or values map.
     * During retrieval of the activity, each placeholder will be replaced with its corresponding
     * reference or value.
     */
    val templateMessageKey: String,

    /**
     * the references to be used to replace placeholders in the message template during retrieval of
     * the activity. A reference refers to a specific aggregate, e.g. a particular user.
     */
    val references: Map<String, ObjectReference> = mapOf(),

    /**
     * the values to be used to replace placeholders in the message template during retrieval of the
     * activity.
     */
    val values: Map<String, String> = mapOf()
)

/** This class contains the information about the task and project that the activity belongs to. */
@TypeAlias("Context") data class Context(val project: UUID, val task: UUID)

/**
 * This class contains the information about an Attachment of the activity, if it exists (i.e. a
 * message attachment)
 */
@TypeAlias("Attachment")
data class Attachment(
    val auditingInformation: AuditingInformation,
    val identifier: UUID,
    val captureDate: LocalDateTime? = null,
    val fileName: String,
    val fileSize: Long,
    val imageHeight: Long,
    val imageWidth: Long,
    val topicId: UUID?,
    val taskId: UUID?,
    val messageId: UUID?
)

data class AuditingInformation(
    val createdBy: UnresolvedObjectReference,
    val createdDate: Date,
    val lastModifiedBy: UnresolvedObjectReference,
    val lastModifiedDate: Date
)
