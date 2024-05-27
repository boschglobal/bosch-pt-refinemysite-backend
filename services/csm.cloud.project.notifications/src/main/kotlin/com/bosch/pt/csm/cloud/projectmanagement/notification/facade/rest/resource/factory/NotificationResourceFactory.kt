/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.common.facade.rest.CustomLinkBuilderFactory
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.DisplayNameResolver
import com.bosch.pt.csm.cloud.projectmanagement.notification.boundary.LazyValueEvaluator
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.NotificationController
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationContextDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationResource
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationResource.Companion.LINK_READ
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.PlaceholderValueDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.CountableAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.MultipleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleDate
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.SingleAttributeChange
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithPlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.TemplateWithValuePlaceholders
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Value
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.boundary.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.project.project.boundary.ProjectService
import com.bosch.pt.csm.cloud.projectmanagement.project.task.boundary.TaskService
import com.bosch.pt.csm.cloud.projectmanagement.user.boundary.UserService
import com.bosch.pt.csm.cloud.projectmanagement.user.facade.rest.ProfilePictureUriBuilder
import com.bosch.pt.csm.cloud.projectmanagement.user.model.User
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringSubstitutor
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class NotificationResourceFactory(
    private val profilePictureUriBuilder: ProfilePictureUriBuilder,
    private val userService: UserService,
    private val participantService: ParticipantService,
    private val messageSource: MessageSource,
    private val projectService: ProjectService,
    private val taskService: TaskService,
    private val displayNameResolver: DisplayNameResolver,
    private val lazyValueEvaluator: LazyValueEvaluator,
    private val linkFactory: CustomLinkBuilderFactory
) {

  fun build(notification: Notification, user: User): NotificationResource {
    val (identifier) =
        participantService.findOneCachedByProjectIdentifierAndUserIdentifier(
            notification.context.project, notification.event.user)

    val actorUser = userService.findOneCachedByIdentifier(notification.event.user)
    val displayName =
        actorUser?.displayName
            ?: messageSource.getMessage(Key.USER_DELETED, null, LocaleContextHolder.getLocale())

    return NotificationResource(
            identifier = notification.externalIdentifier!!,
            read = notification.read,
            actor =
                ResourceReferenceWithPicture(
                    identifier = identifier,
                    displayName = displayName,
                    picture = profilePictureUriBuilder.buildProfilePictureUri(actorUser)),
            date = notification.insertDate,
            summary = buildSummary(notification.summary),
            changes = buildDetails(notification.context, notification.details),
            context = buildContext(notification.context),
            objectReference =
                ObjectReference(
                    type = notification.notificationIdentifier.type,
                    identifier = notification.notificationIdentifier.identifier))
        .apply {
          this.add(
              linkFactory
                  .linkTo(NotificationController.MARK_NOTIFICATION_AS_READ_ENDPOINT)
                  .withParameters(mapOf("notificationId" to notification.externalIdentifier!!))
                  .withRel(LINK_READ))
        }
  }

  private fun buildSummary(notificationSummary: TemplateWithPlaceholders): NotificationSummaryDto {
    // get the display name for those placeholder values that are referencing another aggregate
    val resolvedAggregateReferenceValues =
        notificationSummary.placeholderAggregateReferenceValues
            .map { aggregateReferenceValue ->
              val text = displayNameResolver.resolve(aggregateReferenceValue.value)
              Pair(
                  aggregateReferenceValue.key,
                  PlaceholderValueDto(
                      type = aggregateReferenceValue.value.type,
                      id = aggregateReferenceValue.value.identifier,
                      text = text))
            }
            .toMap()

    return NotificationSummaryDto(
        translateTemplate(notificationSummary), resolvedAggregateReferenceValues)
  }

  private fun buildDetails(context: Context, details: Details?): String? {
    return when (details) {
      null -> null
      is SimpleString -> details.value
      is SingleAttributeChange -> translateSingleAttributeChange(context, details)
      is MultipleAttributeChange -> translateMultipleAttributeChange(details)
      is CountableAttributeChange -> translateCountableAttributeChange(details)
      is TemplateWithPlaceholders -> translateTemplate(details)
      is TemplateWithValuePlaceholders -> translateTemplateWithValue(context, details)
      else -> throw IllegalStateException("Unknown implementation of Details interface")
    }
  }

  private fun buildContext(notificationContext: Context): NotificationContextDto {
    val projectName = projectService.findDisplayName(notificationContext.project)
    var taskName =
        taskService.findDisplayName(notificationContext.task, notificationContext.project)
    taskName = taskName ?: translate(Key.DELETED_TASK)

    return NotificationContextDto(
        ResourceReference(notificationContext.project, projectName),
        ResourceReference(notificationContext.task, taskName))
  }

  private fun translateSingleAttributeChange(context: Context, details: SingleAttributeChange) =
      details.value?.let {
        StringUtils.capitalize(translate(details.attribute)) +
            " \"" +
            renderValue(context, details.value!!) +
            "\""
      }
          ?: run {
            val sub =
                StringSubstitutor(
                    mapOf(Pair("attribute", translate(details.attribute))), "\${", "}")
            StringUtils.capitalize(
                sub.replace(translate(Key.NOTIFICATION_DETAILS_ATTRIBUTE_REMOVED)))
          }

  private fun renderValue(context: Context, value: Value) =
      when (value) {
        is SimpleString -> value.value
        is SimpleDate ->
            value.date.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .localizedBy(LocaleContextHolder.getLocale()))
        is SimpleMessageKey -> translate(value.messageKey)
        is LazyValue -> translate(context.project, value)
        else -> throw IllegalStateException("Unknown implementation of Value interface.")
      }

  private fun translateMultipleAttributeChange(details: MultipleAttributeChange): String {
    var sentence = ""
    for ((index, attribute) in details.attributes.withIndex()) {
      when {
        index == 0 -> sentence += StringUtils.capitalize(translate(attribute))
        index < details.attributes.size - 1 -> {
          sentence += ", "
          sentence += translate(attribute)
        }
        else -> {
          sentence += " "
          sentence += translate(Key.MULTIPLE_ATTRIBUTE_CHANGES_SEPARATOR)
          sentence += " "
          sentence += translate(attribute)
        }
      }
    }
    return sentence
  }

  private fun translateCountableAttributeChange(details: CountableAttributeChange): String? {
    val sub = StringSubstitutor(mapOf(Pair("number", details.value)), "\${", "}")
    return sub.replace(translate(details.message))
  }

  private fun translateTemplate(templateWithPlaceholders: TemplateWithPlaceholders): String {
    val template = translate(templateWithPlaceholders.templateMessageKey)

    // translate those placeholder values that are message keys themselves
    val translatedMessageKeyValuesParameters: Map<String, String> =
        templateWithPlaceholders.placeholderMessageKeyValues
            .map { entry -> Pair(entry.key, translate(entry.value)) }
            .toMap()

    val sub = StringSubstitutor(translatedMessageKeyValuesParameters, "\${", "}")
    return sub.replace(template)
  }

  private fun translateTemplateWithValue(
      context: Context,
      templateWithValuePlaceholders: TemplateWithValuePlaceholders
  ): String {
    val template = translate(templateWithValuePlaceholders.templateMessageKey)

    // translate those placeholder values that are message keys themselves
    val translatedValuesParameters: Map<String, String> =
        templateWithValuePlaceholders.placeholderValues
            .map { entry -> Pair(entry.key, renderValue(context, entry.value)) }
            .toMap()

    val sub = StringSubstitutor(translatedValuesParameters, "\${", "}")
    return sub.replace(template)
  }

  private fun translate(messageKey: String): String =
      messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale())

  private fun translate(projectIdentifier: UUID, value: LazyValue): String =
      lazyValueEvaluator.evaluate(projectIdentifier, value)
}
