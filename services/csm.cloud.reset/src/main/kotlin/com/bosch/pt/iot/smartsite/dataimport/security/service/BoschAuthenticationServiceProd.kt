/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.security.service

import com.bosch.pt.iot.smartsite.common.oauth.utilities.EidpTokenClient
import com.bosch.pt.iot.smartsite.common.oauth.utilities.Environment
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("idp-bosch-prod")
@Service
class BoschAuthenticationServiceProd(
    @Value("\${csm-app-admin-user}") private val adminUser: String,
    @Value("\${csm-app-admin-password}") private val adminPassword: String
) : AuthenticationService {

  private lateinit var accessTokenAdmin: String
  private val accessTokens: MutableMap<String, String> = HashMap()
  private val currentUser = ThreadLocal<String>()

  override fun loginAdmin() {
    accessTokenAdmin =
        "Bearer " + EidpTokenClient.getToken(adminUser, adminPassword, Environment.PROD)
  }

  override fun loginUser(id: String, email: String, password: String) {
    accessTokens[id] = "Bearer " + EidpTokenClient.getToken(email, password, Environment.PROD)
  }

  override val accessToken: String
    get() =
        if (currentUser.get()!!.contentEquals("admin")) accessTokenAdmin
        else
            accessTokens[currentUser.get()]
                ?: throw IllegalStateException(
                    "No access token found for user with id: ${currentUser.get()}")

  override fun selectUser(username: String) = currentUser.set(username)

  override fun selectAdmin() = currentUser.set("admin")
}
