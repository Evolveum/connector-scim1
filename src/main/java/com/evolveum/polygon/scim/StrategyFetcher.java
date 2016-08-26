package com.evolveum.polygon.scim;

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
