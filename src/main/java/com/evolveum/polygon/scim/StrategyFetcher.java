package com.evolveum.polygon.scim;

public class StrategyFetcher {

	public HandlingStrategy fetchStrategy(String providerName) {

		HandlingStrategy strategy;
		String[] uriParts = providerName.split("\\."); // e.g.
		// https://eu6.salesforce.com/services/scim/v1

		if (providerName.contains(".")) {

			if (uriParts.length >= 2) {

				if ("salesforce".equals(uriParts[1])) {
					strategy = new SalesforceHandlingStrategy();

				} else if ("slack".equals(uriParts[1])) {

					strategy = new SlackHandlingStrategy();

				} else {

					strategy = new StandardScimHandlingStrategy();
				}
			} else {

				strategy = new StandardScimHandlingStrategy();
			}

		} else {

			if ("salesforce".equals(providerName)) {
				strategy = new SalesforceHandlingStrategy();

			} else if ("slack".equals(providerName)) {

				strategy = new SlackHandlingStrategy();

			} else {

				strategy = new StandardScimHandlingStrategy();
			}

		}

		return strategy;
	}

}
