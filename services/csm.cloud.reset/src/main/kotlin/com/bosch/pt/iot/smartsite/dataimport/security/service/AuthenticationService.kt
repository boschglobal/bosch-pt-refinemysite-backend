/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.security.service

interface AuthenticationService {
  fun loginAdmin()
  fun loginUser(id: String, email: String, password: String)
  val accessToken: String
  fun selectUser(username: String)
  fun selectAdmin()
}
