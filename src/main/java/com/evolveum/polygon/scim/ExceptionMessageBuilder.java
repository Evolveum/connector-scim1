package com.evolveum.polygon.scim;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExceptionMessageBuilder {
	

	private static final Log LOGGER = Log.getLog(ExceptionMessageBuilder.class);
	
	public static void onNoSuccess(HttpResponse response, String message) throws ParseException, IOException {

		Integer statusCode = null;
		StringBuilder exceptionStringBuilder = null;

		if (response.getEntity() != null) {
			String responseString = EntityUtils.toString(response.getEntity());
			statusCode = response.getStatusLine().getStatusCode();
			LOGGER.error("Full Error response from provider: {0}", responseString);

			JSONObject responseObject = new JSONObject(responseString);

			if (responseObject.has("Errors")) {
				Object returnedObject = new Object();

				returnedObject = responseObject.get("Errors");

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
			exceptionStringBuilder = new StringBuilder("Query for ").append(message)
					.append(" was unsuccessful. No response object was returned");
		}

		String exceptionString = exceptionStringBuilder.toString();

		if (message == null) {
			message = "the full resource representation";
		}
		LOGGER.error(exceptionString);
		if (statusCode != null) {
			LOGGER.info("An error has occured. Http status: \"{0}\"", statusCode);
		}
		LOGGER.info(exceptionString);

		throw new ConnectorIOException(exceptionString);
	}

	public static StringBuilder buildErrorMessage(JSONObject responseObject, String message, int statusCode) {

		String responseString = new String();

		StringBuilder exceptionStringBuilder = new StringBuilder();

		if (responseObject.has("description")) {
			responseString = responseObject.getString("description");
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
