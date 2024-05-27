/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.model

import com.bosch.pt.csm.cloud.projectmanagement.common.repository.Collections.NOTIFICATION
import com.bosch.pt.csm.cloud.projectmanagement.event.model.LocalizableMessage
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = NOTIFICATION)
@TypeAlias("Notification")
data class Notification(
    @Id var notificationIdentifier: NotificationIdentifier,
    var externalIdentifier: UUID? = null,
    var insertDate: Instant = Instant.now(),
    var event: EventInformation,
    var read: Boolean = false,
    var summary: TemplateWithPlaceholders,
    var details: Details? = null,
    var context: Context,
    var merged: Boolean = false
)

/**
 * Unique identifier of a notification. To support idempotent operations it is not just a random
 * UUID. The assumption this identifier is based on is that there will only be a single notification
 * for each event and recipient
 *
 * @param type the type of the aggregate contained in the event the notification arose from
 * @param identifier the identifier of the aggregate contained in the event the notification arose
 *   from
 * @param version the version of the aggregate contained in the event the notification arose from
 * @param recipientIdentifier the UUID of the user the notification is created for
 */
data class NotificationIdentifier(
    var type: String,
    var identifier: UUID,
    var version: Long,
    var recipientIdentifier: UUID
)

/**
 * Contains information about the event that triggered the notification.
 *
 * @param name the name of the event as stated in the AVRO message
 * @param date the date of the event (usually taken from the lastModifiedDate field of the AVRO
 *   message's auditing information)
 * @param user the user that triggered the command the event arose from (usually taken from the
 *   lastModifiedBy field of the AVRO message's auditing information)
 */
data class EventInformation(var name: String, var date: Instant, var user: UUID)

/** This is a marker interface for classes that can be used as notification details. */
interface Details

/**
 * This is a marker interface for classes that can be used as values. This is required to
 * distinguish the value types during rendering when notifications are retrieved.
 */
interface Value

/**
 * Use this class if the notification detail is a fixed string where no rendering needs to be done
 * during retrieval of notifications (i.e. no translations required).
 */
@Document @TypeAlias("SimpleString") data class SimpleString(var value: String) : Details, Value

/**
 * Use this class if the value of an attribute is a date (without time information) and must be
 * rendered accordingly during retrieval of notifications (i.e. language-specific format).
 */
@Document @TypeAlias("SimpleDate") data class SimpleDate(var date: LocalDate) : Value

/**
 * Use this class if the value of an attribute needs to be translated during rendering when
 * retrieving notifications (i.e. enumeration values).
 */
@Document
@TypeAlias("SimpleMessageKey")
data class SimpleMessageKey(var messageKey: String) : Value

/**
 * Use this class if the value of an attribute needs to be resolved during rendering when retrieving
 * activities (e.g. enumeration values).
 */
@Document @TypeAlias("LazyValue") data class LazyValue(var value: Any, var type: String) : Value

/**
 * Use this class if the notification details should state, that a single attribute was changed.
 *
 * @param attribute contains a message key that is translated when notifications are retrieved.
 * @param value contains the new attribute value and can be any implementation of the interface
 *   [Value]"
 */
@Document
@TypeAlias("SingleAttributeChange")
data class SingleAttributeChange(var attribute: String, var value: Value?) : Details

/**
 * Use this class if the notification details should state, that multiple attributes have been
 * changed.
 *
 * @param attributes contains a list of message keys (representing the attribute names) that are
 *   translated when notifications are retrieved.
 */
@Document
@TypeAlias("MultipleAttributeChange")
data class MultipleAttributeChange(var attributes: List<String>) : Details

/**
 * Use this class if the notification detail is a countable change that can potentially be
 * aggregated.
 *
 * @param message contains a message key that MUST have a placeholder ${number} which is replaced
 *   with the value from parameter [value]
 * @param attribute contains a message key that is used when this notification is merged with
 *   another notification having details of type [SingleAttributeChange] or type
 *   [MultipleAttributeChange].
 */
@Document
@TypeAlias("CountableAttributeChange")
data class CountableAttributeChange(var message: String, var attribute: String, var value: Int) :
    Details

/**
 * This class represents a template string (referenced by a message key) that can contain
 * placeholders. These placeholders must be replaced by a value. The value is either the display
 * name of a referenced aggregate (which is then stored in placeholderAggregateValues) or a
 * translated value (whose message key is stored in placeholderMessageKeyValues).
 *
 * @param placeholderAggregateReferenceValues contains placeholder values that reference other
 *   aggregates (i.e. a user). The corresponding display names of the referenced aggregates are
 *   determined during retrieval of notifications
 * @param placeholderMessageKeyValues contains placeholder values that need to be translated during
 *   retrieval of notifications (i.e. enumeration values)
 */
@Document
@TypeAlias("TemplateWithPlaceholders")
data class TemplateWithPlaceholders(
    var templateMessageKey: String,
    var placeholderAggregateReferenceValues: Map<String, ObjectReferenceWithContextRoot> =
        HashMap(),
    var placeholderMessageKeyValues: Map<String, String> = HashMap()
) : Details

@Document
@TypeAlias("TemplateWithValuePlaceholders")
data class TemplateWithValuePlaceholders(
    var templateMessageKey: String,
    var placeholderAggregateReferenceValues: Map<String, ObjectReferenceWithContextRoot> =
        HashMap(),
    var placeholderValues: Map<String, Value> = HashMap()
) : Details

data class ObjectReferenceWithContextRoot(
    var type: String,
    var identifier: UUID,
    // This attribute is used as the shard key in queries to optimize access for sharded containers
    var contextRootIdentifier: UUID
)

data class Context(var project: UUID, var task: UUID)

data class MobileDetails(
    val localizableMessage: LocalizableMessage,
    val data: Map<String, String>,
    val originatingUser: UUID,
    val minVersion: String =
        "1.5.0" // 1.5.0 is the earliest mobile version with any push notifications
)
