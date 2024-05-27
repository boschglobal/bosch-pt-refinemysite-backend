/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.common.test

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import org.junit.jupiter.params.provider.ArgumentsSource

@MustBeDocumented
@Target(FUNCTION)
@Retention(RUNTIME)
@ArgumentsSource(value = FileSourceArgumentsProvider::class)
annotation class FileSource(val container: String = "", val files: Array<String>)
