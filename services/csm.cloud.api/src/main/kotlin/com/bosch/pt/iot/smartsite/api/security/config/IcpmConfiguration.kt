/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.api.security.config

import com.bosch.pt.iot.smartsite.api.security.config.KeyCloak1Configuration.Companion.KEYCLOAK1
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * As SingleKeyID (MYIDP1) is the backing Identity Provider behind KEYCLOAK1 (integrated Customer Profile
 * Management) the MYIDP1 properties are enabled when KEYCLOAK1 is used as identity broker.
 */
@Profile(KEYCLOAK1)
@Configuration
@EnableConfigurationProperties(MyIdp1ConfigurationProperties::class)
@Suppress("UtilityClassWithPublicConstructor")
class KeyCloak1Configuration {

  companion object {
    const val KEYCLOAK1 = "keycloak1"
  }
}
