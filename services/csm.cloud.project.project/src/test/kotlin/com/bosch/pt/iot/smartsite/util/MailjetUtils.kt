/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.util

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.apache.http.HttpStatus.SC_CREATED
import org.json.JSONArray
import org.json.JSONObject

fun RecordedRequest.body(): JSONObject = JSONObject(body.clone().readUtf8())

fun RecordedRequest.messages(): JSONArray = body().getJSONArray("Messages")

fun RecordedRequest.message(): JSONObject = messages().getJSONObject(0)

fun RecordedRequest.ccs(): JSONArray = message().getJSONArray("Cc")

fun RecordedRequest.cc(): JSONObject = ccs().getJSONObject(0)

fun RecordedRequest.bccs(): JSONArray = message().getJSONArray("Bcc")

fun RecordedRequest.bcc(): JSONObject = bccs().getJSONObject(0)

fun RecordedRequest.recipients(): JSONArray = message().getJSONArray("To")

fun RecordedRequest.recipient(): JSONObject = recipients().getJSONObject(0)

fun RecordedRequest.templateId(): Long = message().getLong("TemplateID")

fun RecordedRequest.variables(): JSONObject = message().getJSONObject("Variables")

fun MockWebServer.respondWithSuccess() = respondWithSuccess(1)

fun MockWebServer.respondWithSuccess(times: Int) =
    (1..times).forEach { _ -> enqueue(MockResponse().mailjetSuccess()) }

fun MockWebServer.respondWithError() = enqueue(MockResponse().mailjetError())

fun MockResponse.mailjetSuccess(): MockResponse =
    setResponseCode(SC_CREATED).setBody("{'Messages':[{'Status':'success'}]}")

fun MockResponse.mailjetError(): MockResponse =
    setResponseCode(SC_CREATED)
        .setBody(
            "{'Messages':[" +
                "    {" +
                "      'Status':'error'," +
                "      'Errors':[" +
                "        {" +
                "          'ErrorIdentifier':'f987008f-251a-4dff-8ffc-40f1583ad7bc'," +
                "          'ErrorCode':'mj-0004'," +
                "          'StatusCode':400," +
                "          'ErrorMessage':'Type mismatch. Expected type \\'array of emails\\'.'," +
                "          'ErrorRelatedTo':['HTMLPart','TemplateID']" +
                "        }," +
                "        {" +
                "          'ErrorIdentifier':'8e28ac9c-1fd7-41ad-825f-1d60bc459189'," +
                "          'ErrorCode':'mj-0005'," +
                "          'StatusCode':400," +
                "          'ErrorMessage':'The To is mandatory but missing from the input'," +
                "          'ErrorRelatedTo':['To']" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}")
