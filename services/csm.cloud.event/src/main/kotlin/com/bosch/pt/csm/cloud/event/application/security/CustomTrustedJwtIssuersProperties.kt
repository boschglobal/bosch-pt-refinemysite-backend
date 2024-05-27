/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.event.application.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("custom.security.oauth2.resource-server.jwt")
class CustomTrustedJwtIssuersProperties {
  var issuerUris: List<String> = emptyList()
}
