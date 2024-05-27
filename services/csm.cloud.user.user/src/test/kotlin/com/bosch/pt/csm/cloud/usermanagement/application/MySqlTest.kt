/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application

import com.bosch.pt.csm.cloud.common.mysql.extensions.MySqlTestExtension
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

@Target(CLASS, ANNOTATION_CLASS)
@Retention(RUNTIME)
@ExtendWith(MySqlTestExtension::class)
@AutoConfigureTestDatabase(replace = NONE)
@Tag("user")
annotation class MySqlTest
