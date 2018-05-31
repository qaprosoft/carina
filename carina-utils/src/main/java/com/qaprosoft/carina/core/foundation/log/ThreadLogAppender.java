/*******************************************************************************
 * Copyright 2013-2018 QaProSoft (http://www.qaprosoft.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
 * This appender log groups test outputs by test method/test thread so they don't mess up each other even they runs in parallel.
 */
public class ThreadLogAppender extends AppenderSkeleton {
    // single buffer for each thread test.log file
    private final ThreadLocal<BufferedWriter> testLogBuffer = new ThreadLocal<BufferedWriter>();
    private final ThreadLocal<BufferedWriter> apiLogBuffer = new ThreadLocal<BufferedWriter>();

    @Override
    public void append(LoggingEvent event) {
        // TODO: [VD] OBLIGATORY double check and create separate unit test for this case
        /*
         * if (!ReportContext.isBaseDireCreated()) {
         * System.out.println(event.getMessage().toString());
         * return;
         * }
         */

        try {
            File currentLogFile = new File(ReportContext.getTestDir() + "/test.log");
            BufferedWriter fwlog, fw = testLogBuffer.get();
            BufferedWriter fwapi = apiLogBuffer.get();
            boolean apiMethod = event.getLoggerName().contains("AbstractApiMethod");
            if (apiMethod) {
                fw = fwapi;
                currentLogFile = new File(ReportContext.getTestDir() + "/http.log");
            }

            if (fw == null) {
                // 1st request to log something for this thread/test
                if (!currentLogFile.exists())
                    currentLogFile.createNewFile();
                fw = new BufferedWriter(new FileWriter(currentLogFile, true));
                if (apiMethod) {
                    apiLogBuffer.set(fw);
                } else {
                    testLogBuffer.set(fw);
                }
            }


            if (event != null) {
                // append time, thread, class name and device name if any
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); // 2016-05-26 04:39:16
                String time = dateFormat.format(event.getTimeStamp());
                // System.out.println("time: " + time);

                long threadId = Thread.currentThread().getId();
                // System.out.println("thread: " + threadId);
                String fileName = event.getLocationInformation().getFileName();
                // System.out.println("fileName: " + fileName);

                String logLevel = event.getLevel().toString();

                String message = "[%s] [%s] [%s] [%s] %s";
                fw.write(String.format(message, time, fileName, threadId, logLevel, event.getMessage().toString()));
            } else {
                fw.write("null");
            }
            fw.write("\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void close() {
        try {
            BufferedWriter fw = testLogBuffer.get();
            if (fw != null) {
                fw.close();
                testLogBuffer.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            BufferedWriter fw = apiLogBuffer.get();
            if (fw != null) {
                fw.close();
                apiLogBuffer.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
