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

/**
 * @author Macik
 *
 *         Methods used to pick the right strategy depending form the connected
 *         service.
 *
 */
public class StrategyFetcher {

	private static final String SALESFORCE = "salesforce";
	private static final String SLACK = "slack";

	public HandlingStrategy fetchStrategy(String providerName) {

		HandlingStrategy strategy;
		String[] uriParts = providerName.split("\\."); // e.g.
		// https://eu6.salesforce.com/services/scim/v1

		if (providerName.contains(".")) {

			if (uriParts.length >= 2) {

				if (SALESFORCE.equals(uriParts[1])) {
					strategy = new SalesforceHandlingStrategy();

				} else if (SLACK.equals(uriParts[1])) {

					strategy = new SlackHandlingStrategy();

				} else {

					strategy = new StandardScimHandlingStrategy();
				}
			} else {

				strategy = new StandardScimHandlingStrategy();
			}

		} else {

			if (SALESFORCE.equals(providerName)) {
				strategy = new SalesforceHandlingStrategy();

			} else if (SLACK.equals(providerName)) {

				strategy = new SlackHandlingStrategy();

			} else {

				strategy = new StandardScimHandlingStrategy();
			}

		}

		return strategy;
	}

}
