/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.factory

import com.bosch.pt.csm.cloud.common.extensions.toDate
import com.bosch.pt.csm.cloud.common.i18n.LocalDateFormatter
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ActivityResource
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.ObjectReferenceDto
import com.bosch.pt.csm.cloud.projectmanagement.activity.facade.rest.resources.response.SummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Activity
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.AttributeChanges
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Context
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Details
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.LazyValue
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.NoDetails
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.ResolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleDate
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleMessageKey
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.SimpleString
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.Summary
import com.bosch.pt.csm.cloud.projectmanagement.activity.model.UnresolvedObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.attachment.facade.rest.response.factory.AttachmentResourceFactory
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.aggregate.AggregateType.USER
import com.bosch.pt.csm.cloud.projectmanagement.common.facade.rest.resource.ResourceReferenceWithPicture
import com.bosch.pt.csm.cloud.projectmanagement.common.i18n.Key.USER_DELETED
import com.bosch.pt.csm.cloud.projectmanagement.common.service.DisplayNameResolver
import com.bosch.pt.csm.cloud.projectmanagement.common.service.LazyValueEvaluator
import com.bosch.pt.csm.cloud.projectmanagement.project.participant.service.ParticipantService
import com.bosch.pt.csm.cloud.projectmanagement.user.facade.rest.ProfilePictureUriBuilder
import com.bosch.pt.csm.cloud.projectmanagement.user.service.UserService
import java.util.UUID
import java.util.UUID.randomUUID
import org.apache.commons.text.StringSubstitutor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class ActivityResourceFactory(
    @Value("\${testadmin.user.identifier}") private val testadminUserIdentifier: UUID,
    private val userService: UserService,
    private val profilePictureUriBuilder: ProfilePictureUriBuilder,
    private val messageSource: MessageSource,
    private val displayNameResolver: DisplayNameResolver,
    private val lazyValueEvaluator: LazyValueEvaluator,
    private val attachmentResourceFactory: AttachmentResourceFactory
) {

  fun build(activity: Activity) =
      ActivityResource(
              identifier = activity.identifier,
              user = buildUserResourceReference(activity.event.user),
              date = activity.event.date.toDate(),
              summary = buildSummary(activity.summary),
              details = buildDetails(activity.context, activity.details))
          .apply {
            if (activity.attachment != null) {
              embed(EMBEDDED_ATTACHMENTS, attachmentResourceFactory.build(activity.attachment))
            }
          }

  private fun buildUserResourceReference(eventUserIdentifier: UUID): ResourceReferenceWithPicture {

    val actorUser = userService.findOneCached(eventUserIdentifier)
    val displayName =
        actorUser?.displayName
            ?: messageSource.getMessage(USER_DELETED, null, LocaleContextHolder.getLocale())

    return ResourceReferenceWithPicture(
        identifier = actorUser?.identifier ?: randomUUID(),
        displayName = displayName,
        picture = profilePictureUriBuilder.buildProfilePictureUri(actorUser))
  }

  private fun buildSummary(summary: Summary): SummaryDto {
    val template = summary.values.substituteInTemplate(summary.templateMessageKey)
    val references = summary.references.mapToDtos()

    return SummaryDto(template = template, references = references)
  }

  private fun buildDetails(context: Context, details: Details) =
      when (details) {
        is NoDetails -> emptyList()
        is AttributeChanges -> details.mapToStrings(context)
        else -> throw IllegalStateException("Unknown implementation of Details interface")
      }

  private fun translate(messageKey: String, args: List<String>) =
      messageSource.getMessage(messageKey, args.toTypedArray(), LocaleContextHolder.getLocale())

  private fun translate(messageKey: String) =
      messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale())

  private fun Map<String, String>.substituteInTemplate(templateMessageKey: String): String =
      StringSubstitutor(this, "\${", "}").replace(translate(templateMessageKey))

  private fun Map<String, ObjectReference>.mapToDtos() = mapValues { mapEntry ->
    with(mapEntry.value) {
      when (this) {
        is UnresolvedObjectReference ->
            replaceFakeParticipantWithUserReferenceIfNecessary(this).let {
              ObjectReferenceDto(
                  type = it.type, id = it.identifier, text = displayNameResolver.resolve(it))
            }
        is ResolvedObjectReference ->
            ObjectReferenceDto(type = type, id = identifier, text = displayName)
        else -> throw IllegalStateException("Unknown implementation of object reference")
      }
    }
  }

  private fun AttributeChanges.mapToStrings(context: Context) =
      this.attributes.map { changeDescription ->
        val processedPlaceholderValueArguments =
            changeDescription.values.map {
              when (it) {
                is SimpleString -> it.value
                is SimpleDate ->
                    it.date.format(LocalDateFormatter.forLocale(LocaleContextHolder.getLocale()))
                is SimpleMessageKey -> translate(it.messageKey)
                is UnresolvedObjectReference -> displayNameResolver.resolve(it)
                is LazyValue -> lazyValueEvaluator.evaluate(context.project, it)
                else -> throw IllegalStateException("Unknown value type ${it.javaClass.simpleName}")
              }
            }

        translate(changeDescription.templateMessageKey, processedPlaceholderValueArguments)
      }

  // This is a workaround used to deal with a fake participant unresolved object reference generated
  // when the data was created by the testadmin user, for which a participant does not exist
  private fun replaceFakeParticipantWithUserReferenceIfNecessary(
      unresolvedObjectReference: UnresolvedObjectReference
  ) =
      if (unresolvedObjectReference.type == PARTICIPANT.type &&
          unresolvedObjectReference.identifier == ParticipantService.FAKE_PARTICIPANT_IDENTIFIER) {

        unresolvedObjectReference.copy(type = USER.type, identifier = testadminUserIdentifier)
      } else {
        unresolvedObjectReference
      }

  companion object {
    private const val EMBEDDED_ATTACHMENTS = "attachments"
  }
}
