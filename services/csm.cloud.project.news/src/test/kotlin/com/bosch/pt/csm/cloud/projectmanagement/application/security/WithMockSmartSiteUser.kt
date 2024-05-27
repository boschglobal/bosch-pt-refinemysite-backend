/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.application.security

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import org.springframework.security.test.context.support.WithSecurityContext

/**
 * When used with [WithSecurityContextTestExecutionListener] this annotation can be added to a test
 * method to emulate running with a mocked user. In order to work with [MockMvc] the
 * [SecurityContext] that is used will have the following properties:
 *
 * * The [SecurityContext] created will be that of [ ][SecurityContextHolder.createEmptyContext]
 * * It will be populated with an [UsernamePasswordAuthenticationToken] that uses [ ] with the
 * username of either [.value] or [.userId], [GrantedAuthority] that are specified by [.admin].
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS)
@Retention(RUNTIME)
@Inherited
@MustBeDocumented
@WithSecurityContext(factory = WithMockSmartSiteUserSecurityContextFactory::class)
annotation class WithMockSmartSiteUser(

    /**
     * Convenience mechanism for specifying the username. The default is "user". If [.userId] is
     * specified it will be used instead of [.value]
     *
     * @return username
     */
    val value: String = "user",

    /** Technical database id of the user. */
    val id: Long = 1L,

    /** Flag to set admin permission for the user. */
    val admin: Boolean = false,

    /** Flag to lock the user. */
    val locked: Boolean = false,

    /** The email to be used. The default is "". */
    val email: String = "",

    /** The UUID to be used. */
    val identifier: String = "",

    /**
     * The username to be used. Note that [.value] is a synonym for [.userId], but if [.userId] is
     * specified it will take precedence.
     */
    val userId: String = "user"
)
