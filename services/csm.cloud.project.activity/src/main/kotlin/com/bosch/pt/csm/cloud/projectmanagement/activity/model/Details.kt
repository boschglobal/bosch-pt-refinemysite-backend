/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.activity.model

import java.time.LocalDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

// This is a marker interface for classes that can be used as activity details.
interface Details

/** This class is used to represent an activity that doesn't have details. */
@Document @TypeAlias("NoChanges") class NoDetails : Details

/**
 * This class is used to represent an activity that contains details. The list of details is process
 * in the during retrieval of activity (i.e. task update details).
 */
@Document
@TypeAlias("AttributeChanges")
data class AttributeChanges(val attributes: List<ChangeDescription>) : Details

/**
 *
 * This class represents a single change. A change is described by a template (referenced by a
 * message key). The template can contain placeholders.
 */
@Document
@TypeAlias("ChangeDescription")
data class ChangeDescription(

    /**
     * the message template referenced by a message key. The template can contain placeholders in
     * the form {0}, {1}, and so on. During retrieval of the activity, each placeholder will be
     * replaced with its corresponding value.
     */
    val templateMessageKey: String,

    /**
     * the values to be used to replace placeholders in the message template during retrieval of the
     * activity.
     */
    val values: List<Value>
)

/**
 * This is a marker interface for classes that can be used as values. This is required to
 * distinguish the value types during rendering when activities are retrieved.
 */
interface Value

/**
 * Use this class if the activities detail is a fixed string where no rendering needs to be done
 * during retrieval of activities (i.e. no translations required).
 */
@Document @TypeAlias("SimpleString") data class SimpleString(var value: String) : Value

/**
 * Use this class if the value of an activities is a date (without time information) and must be
 * rendered accordingly during retrieval of activities (i.e. language-specific format).
 */
@Document @TypeAlias("SimpleDate") data class SimpleDate(var date: LocalDate) : Value

/**
 * Use this class if the value of an attribute needs to be translated during rendering when
 * retrieving activities (i.e. enumeration values).
 */
@Document
@TypeAlias("SimpleMessageKey")
data class SimpleMessageKey(var messageKey: String) : Value

/**
 * Use this class if the value of an attribute needs to be resolved during rendering when retrieving
 * activities (e.g. enumeration values).
 */
@Document @TypeAlias("LazyValue") data class LazyValue(var value: Any, var type: String) : Value
