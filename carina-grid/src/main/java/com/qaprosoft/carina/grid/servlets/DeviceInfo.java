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
package com.qaprosoft.carina.grid.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import com.qaprosoft.carina.grid.integration.STF;
import com.qaprosoft.zafira.models.stf.STFDevice;

/**
 * Servlet that retrieves information about STF device.
 * 
 * @author akhursevich
 */
public class DeviceInfo extends RegistryBasedServlet
{
	private static final long serialVersionUID = -4451997550655113756L;

	public DeviceInfo(Registry registry)
	{
		super(registry);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		process(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		process(req, resp);
	}

	protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException 
    {
		response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.setStatus(HttpStatus.SC_NOT_FOUND);
		
		String id = request.getParameter("session");
		if(id != null)
		{
			TestSession session = this.getRegistry().getExistingSession(ExternalSessionKey.fromString(id));
			if(session != null)
			{
				Map<String, Object> cap = session.getSlot().getCapabilities();
				if(cap.containsKey("udid"))
				{
					STFDevice device = STF.getDevice((String) cap.get("udid"));
					if(device != null)
					{
						response.setStatus(HttpStatus.SC_OK);
						response.getWriter().print(device);
					    response.getWriter().close();
					}
				}
			}
		}
    }
}