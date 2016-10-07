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

package com.evolveum.polygon.test.scim;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.identityconnectors.common.logging.Log;
/**
 * 
 * @author Macik
 *
 */

public class PropertiesParser {

	private static final Log LOGGER = Log.getLog(PropertiesParser.class);
	private static List<String> complexDataProvider = new ArrayList<String>();
	private Properties properties;

	static {
		complexDataProvider.add("filterMethodProvider");
	}

	public PropertiesParser(String FilePath) {

		try {
			InputStreamReader fileInputStream = new InputStreamReader(new FileInputStream(FilePath),
					StandardCharsets.UTF_8);
			properties = new Properties();
			properties.load(fileInputStream);
		} catch (FileNotFoundException e) {
			LOGGER.error("File not found: {0}", e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.error("IO exception occurred {0}", e.getLocalizedMessage());
			e.printStackTrace();
		}

	}

	public Object[][] fetchTestData(String dataProviderName) {

		Object[][] dataObject = new Object[0][0];
		int length = 0;

		Map<String, Object> fetchedAttributes = new HashMap<String, Object>();

		for (Object elementName : properties.keySet()) {
			String name = elementName.toString();

			String[] nameParts = name.split("\\_");
			String keyName = nameParts[0];
			String attributeName = nameParts[1];

			if (dataProviderName.equals(keyName)) {
				fetchedAttributes.put(attributeName, properties.get(name));

			}

		}
		length = fetchedAttributes.size();

		if (!complexDataProvider.contains(dataProviderName)) {

			dataObject = new Object[length][2];
			int position = 0;

			for (String attributeName : fetchedAttributes.keySet()) {

				dataObject[position][0] = attributeName;
				dataObject[position][1] = fetchedAttributes.get(attributeName);
				position++;
			}

		} else {
			int position = 0;

			Map<String, String> completeMap = new HashMap<String, String>();

			for (String attributeName : fetchedAttributes.keySet()) {

				String value = (String) fetchedAttributes.get(attributeName);

				String[] nameParts = value.split("\\,");
				for (String part : nameParts) {

					completeMap.put(attributeName, part);
				}

			}
			length = completeMap.size();
			dataObject = new Object[length][2];
			for (String st : completeMap.keySet()) {
				dataObject[position][0] = st;
				dataObject[position][1] = completeMap.get(st);
				position++;
			}

		}

		return dataObject;
	}

}
