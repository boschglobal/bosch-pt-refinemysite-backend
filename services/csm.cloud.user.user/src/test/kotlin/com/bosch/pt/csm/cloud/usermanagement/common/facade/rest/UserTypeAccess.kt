/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.common.facade.rest

import java.util.stream.Stream

class UserTypeAccess private constructor(val userType: String, val isAccessGranted: Boolean) {

  override fun toString(): String = userType + " " + if (isAccessGranted) "granted" else "denied"

  companion object {

    /**
     * @param userTypes define the base of user types that should be tested
     * @param granted define which of the base should have access granted
     * @return a stream of users with either access allowed or not.
     */
    fun createGrantedGroup(userTypes: Array<String>, granted: Set<String>): Stream<UserTypeAccess> =
        userTypes.map { UserTypeAccess(it, granted.contains(it)) }.stream()

    /**
     * @param userTypes define the base of user types that should be tested
     * @param denied define which of the base should have no access granted
     * @return a stream of users with either access allowed or not.
     */
    fun createDeniedGroup(userTypes: Array<String>, denied: Set<String>): Stream<UserTypeAccess> =
        userTypes.map { UserTypeAccess(it, !denied.contains(it)) }.stream()

    fun granted(userType: String) = UserTypeAccess(userType, true)

    fun denied(userType: String) = UserTypeAccess(userType, false)
  }
}
