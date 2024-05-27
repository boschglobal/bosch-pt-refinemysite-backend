/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.notification

import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.NotificationSummaryDto
import com.bosch.pt.csm.cloud.projectmanagement.notification.facade.rest.resource.ObjectReference
import com.bosch.pt.csm.cloud.projectmanagement.notification.model.Notification
import org.hamcrest.Matchers.matchesPattern
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class NotificationVerificationComponent(
    private val messageSource: MessageSource
) {

    private val pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    fun verifyId(resultActions: ResultActions, notification: Notification, index: Int = 0) {
        resultActions.andExpect(
            jsonPath("$.items[$index].id")
                .value(notification.externalIdentifier.toString())
        )
    }

    fun verifyInsertDate(resultActions: ResultActions, notification: Notification, index: Int = 0) {
        resultActions.andExpect(
            jsonPath("$.items[$index].date").value(
                notification.insertDate.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern(pattern))
            )
        )
    }

    fun verifyRead(resultActions: ResultActions, read: Boolean, index: Int = 0) {
        resultActions.andExpect(jsonPath("$.items[$index].read").value(read))
    }

    fun verifyActor(
        resultActions: ResultActions,
        displayName: String,
        identifier: String,
        pictureLinkPattern: String,
        index: Int = 0
    ) {
        resultActions
            .andExpect(
                jsonPath("$.items[$index].actor.displayName")
                    .value(displayName)
            )
            .andExpect(jsonPath("$.items[$index].actor.id").value(identifier))
        verifyLink(resultActions, "$.items[$index].actor.picture", pictureLinkPattern)
    }

    fun verifyDetails(resultActions: ResultActions, value: String?, index: Int = 0) {
        if (value == null) {
            resultActions.andExpect(
                jsonPath("$.items[$index].changes").doesNotExist()
            )
        } else {
            resultActions.andExpect(jsonPath("$.items[$index].changes").value(value))
        }
    }

    fun verifyContextProject(
        resultActions: ResultActions,
        projectIdentifier: String,
        projectName: String,
        index: Int = 0
    ) {
        resultActions
            .andExpect(
                jsonPath("$.items[$index].context.project.id").value(
                    projectIdentifier
                )
            )
            .andExpect(
                jsonPath("$.items[$index].context.project.displayName").value(
                    projectName
                )
            )
    }

    fun verifyContextTask(resultActions: ResultActions, taskIdentifier: String, taskName: String, index: Int = 0) {
        resultActions
            .andExpect(
                jsonPath("$.items[$index].context.task.id").value(
                    taskIdentifier
                )
            )
            .andExpect(
                jsonPath("$.items[$index].context.task.displayName").value(
                    taskName
                )
            )
    }

    fun verifyObject(resultActions: ResultActions, objectReference: ObjectReference, index: Int = 0) {
        resultActions
            .andExpect(
                jsonPath("$.items[$index].object.type").value(objectReference.type)
            )
            .andExpect(
                jsonPath("$.items[$index].object.identifier").value(objectReference.identifier.toString())
            )
    }

    fun verifySummary(resultActions: ResultActions, template: NotificationSummaryDto, index: Int = 0) {
        resultActions.andExpect(
            jsonPath(
                "$.items[$index].${"summary"}.template"
            ).value(translate(template.template))
        )

        resultActions.andExpect(
            jsonPath(
                "$.items[$index].${"summary"}.${"values"}.length()"
            ).value(template.values.size)
        )

        for (pair in template.values) {
            resultActions.andExpect(
                jsonPath("$.items[$index].${"summary"}.values." + pair.key + ".type")
                    .value(pair.value.type)
            )
            resultActions.andExpect(
                jsonPath("$.items[$index].${"summary"}.values." + pair.key + ".id")
                    .value(pair.value.id.toString())
            )
            resultActions.andExpect(
                jsonPath("$.items[$index].${"summary"}.values." + pair.key + ".text")
                    .value(pair.value.text)
            )
        }
    }

    fun verifyLink(resultActions: ResultActions, path: String, linkPattern: String) {
        resultActions.andExpect(jsonPath(path).value(matchesPattern(linkPattern)))
    }

    private fun translate(messageKey: String) =
        messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale())
}
