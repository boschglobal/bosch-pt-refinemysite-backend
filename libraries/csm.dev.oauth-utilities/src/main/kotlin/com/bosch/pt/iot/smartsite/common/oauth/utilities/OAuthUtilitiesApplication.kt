/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.common.oauth.utilities

import com.bosch.pt.iot.smartsite.common.oauth.utilities.EidpTokenClient.getToken
import org.slf4j.LoggerFactory

object OAuthUtilitiesApplication {

  private val LOGGER = LoggerFactory.getLogger(OAuthUtilitiesApplication::class.java)
  private const val NUMBER_OF_PARAMETERS_WITH_CREDENTIALS_AND_ENVIRONMENT = 3
  private const val NUMBER_OF_PARAMETERS_WITH_CREDENTIALS = 2

  @JvmStatic
  fun main(args: Array<String>) {
    val username: String
    val password: String
    var env = Environment.DEV

    if (args.size == NUMBER_OF_PARAMETERS_WITH_CREDENTIALS_AND_ENVIRONMENT) {
      username = args[0]
      password = args[1]
      env = Environment.valueOf(args[2])
    } else if (args.size == NUMBER_OF_PARAMETERS_WITH_CREDENTIALS) {
      username = args[0]
      password = args[1]
    } else if (args.isEmpty()) {
      username = "smartsiteapp+ali@gmail.com"
      password = "Smartali#1"
    } else {
      throw IllegalArgumentException("Invalid amount of parameters provided.")
    }

    LOGGER.warn(getToken(username, password, env))
  }
}
