/*******************************************************************************
 * Copyright 2013-2020 QaProSoft (http://www.qaprosoft.com).
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
package com.qaprosoft.carina.core.foundation.webdriver.listener;

import org.apache.log4j.Logger;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DriverCommand;

import com.qaprosoft.carina.core.foundation.utils.R;

import io.appium.java_client.MobileCommand;
import io.appium.java_client.screenrecording.BaseStartScreenRecordingOptions;
import io.appium.java_client.screenrecording.BaseStopScreenRecordingOptions;
import io.appium.java_client.screenrecording.ScreenRecordingUploadOptions;

/**
 * ScreenRecordingListener - starts/stops video recording for Android and IOS
 * drivers.
 * 
 * @author akhursevich
 */
@SuppressWarnings({ "rawtypes" })
public class MobileRecordingListener<O1 extends BaseStartScreenRecordingOptions, O2 extends BaseStopScreenRecordingOptions>
		implements IDriverCommandListener {

    private static final Logger LOGGER = Logger.getLogger(MobileRecordingListener.class);

	private CommandExecutor commandExecutor;

	private O1 startRecordingOpt;

	private O2 stopRecordingOpt;

	private boolean recording = false;

	public MobileRecordingListener(CommandExecutor commandExecutor, O1 startRecordingOpt, O2 stopRecordingOpt) {
		this.commandExecutor = commandExecutor;
		this.startRecordingOpt = startRecordingOpt;
		this.stopRecordingOpt = stopRecordingOpt;
	}

	@Override
	public void beforeEvent(Command command) {
		if (recording) {
			if (DriverCommand.QUIT.equals(command.getName())) {
                try {
                    String sessionId = command.getSessionId().toString();
                    LOGGER.debug("Stopping mobile video recording and upload data locally for " + sessionId);
                    stopRecordingOpt.withUploadOptions(new ScreenRecordingUploadOptions()
                            .withRemotePath(String.format(R.CONFIG.get("screen_record_ftp"), sessionId))
                            .withAuthCredentials(R.CONFIG.get("screen_record_user"), R.CONFIG.get("screen_record_pass")));
                    
                    // .withRemotePath(String.format(R.CONFIG.get("screen_record_ftp"), "artifacts/test-sessions/" + sessionId + "/video.mp4"))
                    
                    commandExecutor
                            .execute(new Command(command.getSessionId(), MobileCommand.STOP_RECORDING_SCREEN,
                                    MobileCommand.stopRecordingScreenCommand(
                                            (BaseStopScreenRecordingOptions) stopRecordingOpt).getValue()));
                    
                    LOGGER.debug("Stopped mobile video recording and uploaded data locally for " + sessionId);
                } catch (Throwable e) {
                    LOGGER.error("Unable to stop screen recording!", e);
                }
			}
		}
	}

    @Override
    public void afterEvent(Command command) {
        if (!recording && command.getSessionId() != null) {
            try {
                recording = true;
                
                commandExecutor.execute(new Command(command.getSessionId(), MobileCommand.START_RECORDING_SCREEN,
                        MobileCommand.startRecordingScreenCommand((BaseStartScreenRecordingOptions) startRecordingOpt)
                                .getValue()));
            } catch (Exception e) {
                LOGGER.error("Unable to start screen recording: " + e.getMessage(), e);
            }
        }
    }
    
}
