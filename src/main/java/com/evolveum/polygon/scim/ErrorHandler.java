/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.polygon.scim;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Macik
 *
 *         Contains methods used to create and build error messages and
 *         exceptions in case of an error.
 *
 */
public class ErrorHandler {

	private static final String ERRORS = "Errors";
	private static final String DESCRIPTION = "description";
	private static final String MESSAGE = "message";

	private static final Log LOGGER = Log.getLog(ErrorHandler.class);

	/**
	 * Processes the provided parameters from which it creates error messages
	 * describing the arisen error situation.
	 * 
	 * @param response
	 *            The HTTP query response object returned from the service
	 *            provider.
	 * @param message
	 *            Additional string message providing and description of the
	 *            error.
	 * @throws ParseException
	 * @throws IOException
	 */
	public static String onNoSuccess(HttpResponse response, String message) throws ParseException, IOException {

		boolean isJsonObject = true;
		Integer statusCode = null;
		StringBuilder exceptionStringBuilder = null;

		if (response.getEntity() != null) {
			String responseString = EntityUtils.toString(response.getEntity());
			statusCode = response.getStatusLine().getStatusCode();
			LOGGER.error("Full Error response from the provider: {0}", responseString);

			try {
				new JSONObject(responseString);
			} catch (JSONException ex) {
				isJsonObject = false;

			}
			try {

				new JSONArray(responseString);
			} catch (JSONException arrayE) {

				isJsonObject = true;
			}

			if (isJsonObject) {

				JSONObject responseObject = new JSONObject(responseString);

				if (responseObject.has(ERRORS)) {
					Object returnedObject = new Object();

					returnedObject = responseObject.get(ERRORS);

					if (returnedObject instanceof JSONObject) {

						responseObject = (JSONObject) returnedObject;

						exceptionStringBuilder = buildErrorMessage(responseObject, message, statusCode);
					} else if (returnedObject instanceof JSONArray) {

						for (Object messageObject : (JSONArray) returnedObject) {
							exceptionStringBuilder = buildErrorMessage((JSONObject) messageObject, message, statusCode);

						}

					}

				} else {

					exceptionStringBuilder = new StringBuilder("Query for ").append(message)
							.append(" was unsuccessful. Status code returned: ").append(statusCode);
				}
			} else {

				JSONArray responseObject = new JSONArray(responseString);

				for (Object messageObject : (JSONArray) responseObject) {
					exceptionStringBuilder = buildErrorMessage((JSONObject) messageObject, message, statusCode);

				}
			}
		} else {
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. No response object was returned");
		}
		String exceptionString = exceptionStringBuilder.toString();

		if (message == null) {
			message = "the full resource representation";
		}
		LOGGER.error(exceptionString);
		if (statusCode != null) {
			LOGGER.info("An error has occurred. HTTP status: \"{0}\"", statusCode);
		}
		LOGGER.info(exceptionString);
		return exceptionString;
	}

	/**
	 * Builds an error message depending on the response from the provider. If
	 * there in no description in the response then the error message will
	 * switch to a more generic content.
	 * 
	 * @param responseObject
	 *            The json response from the resource provider.
	 * @param message
	 *            Additional string message providing and description of the
	 *            error.
	 * @param statusCode
	 *            The response status code returned from the service provider.
	 * @return the build error message.
	 */
	public static StringBuilder buildErrorMessage(JSONObject responseObject, String message, int statusCode) {

		String responseString = new String();

		StringBuilder exceptionStringBuilder = new StringBuilder();

		if (responseObject.has(DESCRIPTION)) {
			responseString = responseObject.getString(DESCRIPTION);
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. Status code returned: ").append("\"").append(statusCode).append("\"")
					.append(". Error response from provider: ").append("\"").append(responseString).append("\"");

		} else if (responseObject.has(MESSAGE)) {

			responseString = responseObject.getString(MESSAGE);
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. Status code returned: ").append("\"").append(statusCode).append("\"")
					.append(". Error response from provider: ").append("\"").append(responseString).append("\"");

		} else {
			responseString = ". No description was provided from the provider";
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. Status code returned: ").append("\"").append(statusCode).append("\"")
					.append(responseString);
		}

		return exceptionStringBuilder;
	}

}
