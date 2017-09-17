package com.qaprosoft.carina.core.foundation.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import com.qaprosoft.carina.core.foundation.report.ReportContext;

/*
 *  This appender logs groups test outputs by test method so they don't mess up each other even they runs in parallel.
 */
public class ThreadLogAppender extends AppenderSkeleton
{
	//single buffer for each thread test.log file 
	private final ThreadLocal<BufferedWriter> testLogBuffer = new ThreadLocal<BufferedWriter> (); 


	@Override
	public synchronized void append(LoggingEvent event)
	{
		//TODO: [VD] OBLIGATORY double check and create separate unit test for this case
/*		if (!ReportContext.isBaseDireCreated()) {
			System.out.println(event.getMessage().toString());
			return;
		}*/
		
		try
		{
			
			BufferedWriter fw = testLogBuffer.get();
			if (fw == null) {
				// 1st request to log something for this thread/test
			    File testLogFile = new File(ReportContext.getTestDir() + "/test.log");
				if (!testLogFile.exists())
					testLogFile.createNewFile();
				fw = new BufferedWriter(new FileWriter(testLogFile, true));
				testLogBuffer.set(fw);
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
				
				

				// TODO: review and implement valid device name logging
				// String deviceName = DevicePool.getDevice().getName();
				String deviceName = "";
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
	
	@Override
	public void close()
	{
		try
		{
			BufferedWriter fw = testLogBuffer.get();
			if (fw != null) {
				fw.close();
				testLogBuffer.remove();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean requiresLayout()
	{
		return false;
	}
}
