/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.model

data class MobilePushNotificationCommand(
    val recipients: List<String>,
    val message: LocalizableMessage,
    val data: Map<String, String>,
    val originator: String,
    val minVersion: String
)

data class LocalizableMessage(val title: LocalizableText, val body: LocalizableText)

data class LocalizableText(val key: String, val localizationArgs: List<String> = emptyList())
