/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.security

import java.lang.annotation.Inherited
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener
import org.springframework.test.web.servlet.MockMvc

/**
 * When used with [WithSecurityContextTestExecutionListener] this annotation can be added to a test
 * method to emulate running with a mocked user. In order to work with [MockMvc] the
 * [SecurityContext] that is used will have the following properties:
 *
 * * The [SecurityContext] created will be that of [ ][SecurityContextHolder.createEmptyContext]
 * * It will be populated with an [UsernamePasswordAuthenticationToken] that uses [ ] with the
 * username of either [.value] or [.username], [GrantedAuthority] that are specified by [.roles].
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
@WithSecurityContext(factory = WithMockSmartSiteUserSecurityContextFactory::class)
annotation class WithMockSmartSiteUser(

    /**
     * Convenience mechanism for specifying the username. The default is "user". If [ ][.username]
     * is specified it will be used instead of [.value]
     *
     * @return username
     */
    val value: String = "user",

    /** Database id of the user. */
    val id: Long = 1L,

    /**
     * The username to be used. Note that [.value] is a synonym for [.username], but if [.username]
     * is specified it will take precedence.
     */
    val username: String = "user",

    /**
     * The roles to use. The default is "USER". A [GrantedAuthority] will be created for each value
     * within roles. Each value in roles will automatically be prefixed with "ROLE_". For example,
     * the default will result in "ROLE_USER" being used.
     */
    val roles: Array<String> = ["USER"],

    /** The email to be used. The default is "". */
    val email: String = "",

    /** The identifier as UUID to be used. */
    val identifier: String = "",

    /** The user should have role ADMIN. */
    val isAdmin: Boolean = false,

    /** The user should be locked. */
    val isLocked: Boolean = false
)
