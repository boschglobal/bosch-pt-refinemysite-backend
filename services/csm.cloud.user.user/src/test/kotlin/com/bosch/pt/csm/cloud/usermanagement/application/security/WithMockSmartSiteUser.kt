/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.security

import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum
import com.bosch.pt.csm.cloud.usermanagement.user.user.GenderEnum.MALE
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
 * * The [SecurityContext] created with be that of [ ][SecurityContextHolder.createEmptyContext]
 * * It will be populated with an [UsernamePasswordAuthenticationToken] that uses [ ] with the
 * username of either [.value] or [.username], [GrantedAuthority] that are specified by [.admin].
 */
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, ANNOTATION_CLASS, CLASS)
@Retention(RUNTIME)
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

    /**
     * Technical id of the user.
     *
     * @return id
     */
    val id: Long = 1L,

    /**
     * The username to be used. Note that [.value] is a synonym for [.username], but if [.username]
     * is specified it will take precedence.
     *
     * @return username
     */
    val username: String = "user",

    /**
     * Flag to indicate whether user is admin or normal user.
     *
     * @return the flag
     */
    val admin: Boolean = false,

    /**
     * The first name to be used. The default is "".
     *
     * @return the first name
     */
    val firstName: String = "",

    /**
     * The last name to be used. The default is "".
     *
     * @return the last name
     */
    val lastName: String = "",

    /**
     * The email to be used. The default is "".
     *
     * @return the email
     */
    val email: String = "",

    /**
     * The gender to be used. The default is [GenderEnum.MALE].
     *
     * @return the gender
     */
    val gender: GenderEnum = MALE,

    /**
     * The UUID to be used.
     *
     * @return the UUID as string
     */
    val identifier: String = "",

    /**
     * Optional locale preference of the user (e.g. en_GB)
     *
     * @return the locale as string
     */
    val userLocale: String = ""
)
