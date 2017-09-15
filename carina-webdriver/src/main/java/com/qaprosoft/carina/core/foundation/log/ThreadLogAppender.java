package com.qaprosoft.carina.core.foundation.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import com.qaprosoft.carina.core.foundation.report.ReportContext;
import com.qaprosoft.carina.core.foundation.utils.naming.TestNamingUtil;
import com.qaprosoft.carina.core.foundation.webdriver.device.DevicePool;

/*
 *  This appender logs groups test outputs by test method so they don't mess up each other even they runs in parallel.
 */
public class ThreadLogAppender extends AppenderSkeleton
{
	private final ConcurrentHashMap<String, BufferedWriter> test2file = new ConcurrentHashMap<String, BufferedWriter>();

	@Override
	public synchronized void append(LoggingEvent event)
	{
		if (!ReportContext.isBaseDireCreated()) {
			System.out.println(event.getMessage().toString());
			return;
		}
		try
		{
			String test = "";
			if (TestNamingUtil.isTestNameRegistered()) {
				test = TestNamingUtil.getTestNameByThread();
			} else {
				test = TestNamingUtil.getCanonicTestNameByThread();
			}

			if (test == null || StringUtils.isEmpty(test)) {
				System.out.println(event.getMessage().toString());
				//don't write any message into the log if thread is not associated anymore with test
				return;
			}
			
			BufferedWriter fw = test2file.get(test);
			if (fw == null)
			{
			    File testLogFile = new File(ReportContext.getTestDir(test) + "/test.log");
			    if (!testLogFile.exists()) testLogFile.createNewFile();
				fw = new BufferedWriter(new FileWriter(testLogFile));
				test2file.put(test, fw);
			}
			if (event != null) {
				//append time, thread, class name and device name if any
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); //2016-05-26 04:39:16
				String time = dateFormat.format(event.getTimeStamp());
				//System.out.println("time: " + time);
				
				long threadId = Thread.currentThread().getId();
				//System.out.println("thread: " + threadId);
				String fileName = event.getLocationInformation().getFileName();
				//System.out.println("fileName: " + fileName);
				
				String logLevel = event.getLevel().toString();
				
				

				String deviceName = DevicePool.getDevice().getName();
				if (!deviceName.isEmpty()) {
					deviceName = " [" + deviceName + "] ";
				}
				
				String message = "[%s] [%s] [%s] [%s]%s %s";
				fw.write(String.format(message, time, fileName, threadId, logLevel, deviceName, event.getMessage().toString()));
			} else {
				fw.write("null");
			}
			fw.write("\n");
			fw.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public void closeResource(String test)
	{
		try
		{
			if (test2file.get(test) != null) {
				test2file.get(test).close();
				test2file.remove(test);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void close()
	{
	}

	@Override
	public boolean requiresLayout()
	{
		return false;
	}
}
