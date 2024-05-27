/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import java.lang.annotation.Inherited

/**
 * When used with [WithSecurityContextTestExecutionListener] this annotation can be added to a
 * test method to emulate running with a mocked user. In order to work with [MockMvc] the
 * [SecurityContext] that is used will have the following properties:
 *
 * The [SecurityContext] created will be that of [SecurityContextHolder.createEmptyContext]
 * It will be populated with an [UsernamePasswordAuthenticationToken] that uses an
 * notification service user with the username of either [.value] or [.externalIdentifier],
 * [GrantedAuthority] that are specified by setting the [.admin] field.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS,
    AnnotationTarget.FILE
)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
@WithSecurityContext(factory = WithMockSmartSiteUserSecurityContextFactory::class)
annotation class WithMockSmartSiteUser(
    val value: String = "user", // User ID from Bosch CIAM
    val externalIdentifier: String = "user",
    val identifier: String = "",
    val userIdentifier: String = "",
    val admin: Boolean = false
)
