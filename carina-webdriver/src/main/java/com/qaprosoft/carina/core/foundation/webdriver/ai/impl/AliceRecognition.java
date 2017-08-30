/*
 * Copyright 2013-2017 QAPROSOFT (http://qaprosoft.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qaprosoft.carina.core.foundation.webdriver.ai.impl;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.tree.MergeCombiner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qaprosoft.alice.client.AliceClient;
import com.qaprosoft.alice.client.AliceClient.Response;
import com.qaprosoft.alice.models.dto.RecognitionMetaType;
import com.qaprosoft.carina.core.foundation.webdriver.ai.IRecognition;
import com.qaprosoft.carina.core.foundation.webdriver.ai.Label;

/**
 * AliceRecognition - initializes Alice HTTP client and processes response results.
 * 
 * @author akhursevich
 */
public class AliceRecognition implements IRecognition
{
	public static final AliceRecognition INSTANCE = new AliceRecognition();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AliceRecognition.class);
	
	private static final String ALICE_PROPERTIES = "alice.properties";
	private static final String ALICE_ENABLED = "alice_enabled";
	private static final String ALICE_SERVICE_URL = "alice_service_url";
	private static final String ALICE_ACCESS_TOKEN = "alice_access_token";
	
	private boolean enabled = false;
	
	private AliceClient client;
	
	private AliceRecognition()
	{
		try
		{
			CombinedConfiguration config = new CombinedConfiguration(new MergeCombiner());
			config.addConfiguration(new SystemConfiguration());
			config.addConfiguration(new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
				    					  .configure(new Parameters().properties().setFileName(ALICE_PROPERTIES)).getConfiguration());
				
			this.enabled = config.getBoolean(ALICE_ENABLED, false);
			String url = config.getString(ALICE_SERVICE_URL, null); 
			String accessToken = config.getString(ALICE_ACCESS_TOKEN, null); 
		
			if(enabled && !StringUtils.isEmpty(url) && !StringUtils.isEmpty(accessToken))
			{
				this.client = new AliceClient(url);
				this.client.setAuthToken(accessToken);
				this.enabled = this.client.isAvailable();
			}
		}
		catch (Exception e) 
		{
			LOGGER.error("Unable to initialize Alice: " + e.getMessage());
		}
	};
	
	@Override
	public RecognitionMetaType recognize(Label label, String caption, File screenshot)
	{
		RecognitionMetaType result = null;
		
		Response<List<RecognitionMetaType>> response = client.recognize(screenshot);
		
		if(response.getStatus() == 200) 
		{
			result = response.getObject().stream()
				.filter(r -> r.getCaption().contains(caption) && r.getLabel().equals(label.getLabelName()))
				.findAny().orElse(null);
		}
		return result;
	}

	public boolean isEnabled()
	{
		return enabled;
	}
}