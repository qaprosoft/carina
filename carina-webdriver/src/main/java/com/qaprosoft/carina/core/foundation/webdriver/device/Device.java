package com.qaprosoft.carina.core.foundation.webdriver.device;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;
import com.qaprosoft.carina.core.foundation.utils.R;
import com.qaprosoft.carina.core.foundation.utils.SpecialKeywords;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.exception.ExecutorException;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.utils.AdbExecutor;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.utils.CmdLine;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.utils.Platform;
import com.qaprosoft.carina.core.foundation.utils.android.recorder.utils.ProcessBuilderExecutor;
import com.qaprosoft.carina.core.foundation.utils.factory.DeviceType;
import com.qaprosoft.carina.core.foundation.utils.factory.DeviceType.Type;
import com.qaprosoft.carina.core.foundation.webdriver.appium.status.AppiumStatus;

//Motorola|ANDROID|4.4|T01130FJAD|http://localhost:4725/wd/hub;Samsung_S4|ANDROID|4.4.2|5ece160b|http://localhost:4729/wd/hub;
public class Device
{
	private static final Logger LOGGER = Logger.getLogger(Device.class);

	private String name;
	private String type;

	private String os;
	private String osVersion;
	private String udid;
	private String seleniumServer;

	private String testId;
	private String remoteURL;
	
	private boolean isAppInstalled = false;
	
	AdbExecutor executor = new AdbExecutor();

	public Device(String args)
	{
		// Samsung_S4|ANDROID|4.4.2|5ece160b|4729|4730|http://localhost:4725/wd/hub
		LOGGER.debug("mobile_device_args: " + args);
		args = args.replaceAll("&#124", "|");
		LOGGER.debug("mobile_device_args: " + args);

		String[] params = args.split("\\|");

		// TODO: organize verification onto the params count
		this.name = params[0];
		LOGGER.debug("mobile_device_name: " + name);
		this.type = params[1];
		LOGGER.debug("mobile_device_type: " + params[1]);
		this.os = params[2];
		this.osVersion = params[3];
		this.udid = params[4];
		this.seleniumServer = params[5];
	}
	
	public Device()
	{
		this(null, null, null, null, null, null);
	}

	public Device(String name, String type, String os, String osVersion, String udid, String seleniumServer)
	{
		this.name = name;
		this.type = type;
		this.os = os;
		this.osVersion = osVersion;
		this.udid = udid;
		this.seleniumServer = seleniumServer;

	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getOs()
	{
		return os;
	}

	public void setOs(String os)
	{
		this.os = os;
	}

	public String getOsVersion()
	{
		return osVersion;
	}

	public void setOsVersion(String osVersion)
	{
		this.osVersion = osVersion;
	}

	public String getUdid()
	{
		return udid;
	}

	public void setUdid(String udid)
	{
		this.udid = udid;
	}

	public String getSeleniumServer()
	{
		return seleniumServer;
	}

	public void setSeleniumServer(String seleniumServer)
	{
		this.seleniumServer = seleniumServer;
	}
	
	public String getRemoteURL() {
		return remoteURL;
	}

	public void setRemoteURL(String remoteURL) {
		this.remoteURL = remoteURL;
	}

	public boolean isPhone()
	{
		return type.equalsIgnoreCase(SpecialKeywords.PHONE);
	}

	public boolean isTablet()
	{
		return type.equalsIgnoreCase(SpecialKeywords.TABLET);
	}

	public boolean isTv()
	{
		return type.equalsIgnoreCase(SpecialKeywords.TV);
	}

	public String getTestId()
	{
		return testId;
	}

	public void setTestId(String testId)
	{
		this.testId = testId;
	}

	public Type getType()
	{
		if (os.equalsIgnoreCase(SpecialKeywords.ANDROID))
		{
			if (isTablet())
			{
				return Type.ANDROID_TABLET;
			}
			if (isTv())
			{
				return Type.ANDROID_TV;
			}
			return Type.ANDROID_PHONE;
		} 
		else if (os.equalsIgnoreCase(SpecialKeywords.IOS))
		{
			if (isTablet())
			{
				return Type.IOS_TABLET;
			}
			return Type.IOS_PHONE;
		}
		throw new RuntimeException("Incorrect driver type. Please, check config file.");
	}
	
	public boolean isNull() {
		if (name == null || seleniumServer == null) {
			return true;
		}
		return name.isEmpty() || seleniumServer.isEmpty();
	}

	public void connectRemote() {
		if (isNull())
			return;

		LOGGER.info("adb connect " + getRemoteURL());
		String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "connect", getRemoteURL());
		executor.execute(cmd);
	}
	
	public void disconnectRemote() {
		if (isNull())
			return;

		LOGGER.info("adb disconnect " + getRemoteURL());
		String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "disconnect", getRemoteURL());
		executor.execute(cmd);
	}
	
	
    public int startRecording(String pathToFile) {
        if (!Configuration.getBoolean(Parameter.VIDEO_RECORDING)) {
            return -1;
        }
        
        if (this.isNull())
        	return -1;
        
        dropFile(pathToFile);

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "screenrecord", "--bit-rate", "1000000", "--verbose", pathToFile);

        try {
            ProcessBuilderExecutor pb = new ProcessBuilderExecutor(cmd);

            pb.start();
            return pb.getPID();

        } catch (ExecutorException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    public void stopRecording(Integer pid) {
        if (isNull())
        	return;
        
        if (pid != null && pid != -1) {
            Platform.killProcesses(Arrays.asList(pid));
        }
    }
    
    public void dropFile(String pathToFile) {
        if (this.isNull())
        	return;

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "rm", pathToFile);
        executor.execute(cmd);
    }
    
    public String getFullPackageByName(final String name) {

        List<String> packagesList = getInstalledPackages();
        LOGGER.info("Found packages: ".concat(packagesList.toString()));
        String resultPackage = null;
        for (String packageStr : packagesList) {
            if (packageStr.matches(String.format(".*%s.*", name))) {
                LOGGER.info("Package was found: ".concat(packageStr));
                resultPackage = packageStr;
                break;
            }
        }
        if (null == resultPackage) {
            LOGGER.info("Package wasn't found using following name: ".concat(name));
            resultPackage = "not found";
        }
        return resultPackage;
    }
    
    public List<String> getInstalledPackages() {
        String deviceUdid = getAdbName();
        LOGGER.info("Device udid: ".concat(deviceUdid));
        String[] cmd = CmdLine.createPlatformDependentCommandLine("adb", "-s", deviceUdid, "shell", "pm", "list", "packages");
        LOGGER.info("Following cmd will be executed: " + Arrays.toString(cmd));
        List<String> packagesList = executor.execute(cmd);
        return packagesList;
    }

    public boolean isAppInstall(final String packageName) {
        return !getFullPackageByName(packageName).contains("not found");
    }
    
    public void pullFile(String pathFrom, String pathTo) {
        if (isNull())
        	return;

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "pull", pathFrom, pathTo);
        executor.execute(cmd);
    }
    
    
    
    private Boolean getScreenState() {
        // determine current screen status
        // adb -s <udid> shell dumpsys input_method | find "mScreenOn"
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "dumpsys",
                "input_method");
        List<String> output = executor.execute(cmd);

        Boolean screenState = null;
        String line;

        Iterator<String> itr = output.iterator();
        while (itr.hasNext()) {
            // mScreenOn - default value for the most of Android devices
            // mInteractive - for Nexuses
            line = itr.next();
            if (line.contains("mScreenOn=true") || line.contains("mInteractive=true")) {
                screenState = true;
                break;
            }
            if (line.contains("mScreenOn=false") || line.contains("mInteractive=false")) {
                screenState = false;
                break;
            }
        }

        if (screenState == null) {
            LOGGER.error(udid
                    + ": Unable to determine existing device screen state!");
            return screenState; //no actions required if state is not recognized.
        }

        if (screenState) {
            LOGGER.info(udid + ": screen is ON");
        }

        if (!screenState) {
            LOGGER.info(udid + ": screen is OFF");
        }

        return screenState;
    }


    public void screenOff() {
        if (!Configuration.get(Parameter.MOBILE_PLATFORM_NAME).equalsIgnoreCase(SpecialKeywords.ANDROID)) {
            return;
        }
        if (!Configuration.getBoolean(Parameter.MOBILE_SCREEN_SWITCHER)) {
            return;
        }
        
        if (isNull())
        	return;

        Boolean screenState = getScreenState();
        if (screenState == null) {
            return;
        }
        if (screenState) {
			String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "input",
					"keyevent", "26");
            executor.execute(cmd);

            pause(5);

            screenState = getScreenState();
            if (screenState) {
                LOGGER.error(udid + ": screen is still ON!");
            }

            if (!screenState) {
                LOGGER.info(udid + ": screen turned off.");
            }
        }
    }


    public void screenOn() {
        if (!Configuration.get(Parameter.MOBILE_PLATFORM_NAME).equalsIgnoreCase(SpecialKeywords.ANDROID)) {
            return;
        }

        if (!Configuration.getBoolean(Parameter.MOBILE_SCREEN_SWITCHER)) {
            return;
        }

        if (isNull())
        	return;
        
        Boolean screenState = getScreenState();
        if (screenState == null) {
            return;
        }

        if (!screenState) {
            String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell",
                    "input", "keyevent", "26");
            executor.execute(cmd);

            pause(5);
            // verify that screen is Off now
            screenState = getScreenState();
            if (!screenState) {
                LOGGER.error(udid + ": screen is still OFF!");
            }

            if (screenState) {
                LOGGER.info(udid + ": screen turned on.");
            }
        }
    }
    

	public void pressKey(int key) {
		if (isNull())
			return;

		String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "input",
				"keyevent", String.valueOf(key));
		executor.execute(cmd);
	}
    
    public void pause(long timeout) {
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public void clearAppData() {
    	clearAppData(Configuration.get(Parameter.MOBILE_APP));
    }
    
    public void clearAppData(String app) {
        if (!Configuration.get(Parameter.MOBILE_PLATFORM_NAME).equalsIgnoreCase(SpecialKeywords.ANDROID)) {
            return;
        }
        
        if (!Configuration.getBoolean(Parameter.MOBILE_APP_CLEAR_CACHE))
            return;

        if (isNull())
        	return;

        //adb -s UDID shell pm clear com.myfitnesspal.android
        String packageName = getApkPackageName(app);

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "pm", "clear", packageName);
        executor.execute(cmd);
    }
    
    public String getApkPackageName(String apkFile) {
        // aapt dump badging <apk_file> | grep versionCode
        // aapt dump badging <apk_file> | grep versionName
        // output:
        // package: name='com.myfitnesspal.android' versionCode='9025' versionName='develop-QA' platformBuildVersionName='6.0-2704002'

        String packageName = "";
        
        String[] cmd = CmdLine.insertCommandsAfter("aapt dump badging".split(" "), apkFile);
        List<String> output = executor.execute(cmd);
        // parse output command and get appropriate data


        for (String line : output) {
            if (line.contains("versionCode") && line.contains("versionName")) {
                LOGGER.debug(line);
                String[] outputs = line.split("'");
                packageName = outputs[1]; //package
            }
        }

        return packageName;
    }
    
    public void uninstallApp(String packageName) {
        if (isNull())
        	return;

        //adb -s UDID uninstall com.myfitnesspal.android
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "uninstall", packageName);
        executor.execute(cmd);
    }

    public void installApp(String apkPath) {
        if (isNull())
        	return;

        //adb -s UDID install com.myfitnesspal.android
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "install", "-r", apkPath);
        executor.execute(cmd);
    }

    public synchronized void installAppSync(String apkPath) {
        if (isNull())
        	return;

        //adb -s UDID install com.myfitnesspal.android
        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "install", "-r", apkPath);
        executor.execute(cmd);
    }
    
    public void reinstallApp() {
        if (!Configuration.get(Parameter.MOBILE_PLATFORM_NAME).equalsIgnoreCase(SpecialKeywords.ANDROID)) {
            return;
        }

        if (isNull())
        	return;
        
        String mobileApp = Configuration.get(Parameter.MOBILE_APP);
        String oldMobileApp = Configuration.get(Parameter.MOBILE_APP_PREUPGRADE);
        
		if (!oldMobileApp.isEmpty()) {
			//redefine strategy to do upgrade scenario
			R.CONFIG.put(Parameter.MOBILE_APP_UNINSTALL.getKey(), "true");
			R.CONFIG.put(Parameter.MOBILE_APP_INSTALL.getKey(), "true");
		}

        if (Configuration.getBoolean(Parameter.MOBILE_APP_UNINSTALL)) {
            // explicit reinstall the apk
            String[] apkVersions = getApkVersion(mobileApp); // Configuration.get(Parameter.MOBILE_APP)
            if (apkVersions != null) {
                String appPackage = apkVersions[0];

                String[] apkInstalledVersions = getInstalledApkVersion(appPackage);

                LOGGER.info("installed app: " + apkInstalledVersions[2] + "-" + apkInstalledVersions[1]);
                LOGGER.info("new app: " + apkVersions[2] + "-" + apkVersions[1]);

                if (apkVersions[1].equals(apkInstalledVersions[1]) && apkVersions[2].equals(apkInstalledVersions[2]) && oldMobileApp.isEmpty()) {
                    LOGGER.info(
                            "Skip application uninstall and cache cleanup as exactly the same version is already installed.");
                } else {
                    uninstallApp(appPackage);
                    clearAppData(appPackage);
                    isAppInstalled = false;
                    if (!oldMobileApp.isEmpty()) {
                    	LOGGER.info("Starting sync install operation for preupgrade app: " + oldMobileApp);
                    	installAppSync(oldMobileApp);
                    }
                    
                    if (Configuration.getBoolean(Parameter.MOBILE_APP_INSTALL)) {
                        // install application in single thread to fix issue with gray Google maps
                    	LOGGER.info("Starting sync install operation for app: " + mobileApp);
                    	installAppSync(mobileApp);
                    }
                }
            }
        } else if (Configuration.getBoolean(Parameter.MOBILE_APP_INSTALL) && !isAppInstalled) {
        	LOGGER.info("Starting install operation for app: " + mobileApp);
        	installApp(mobileApp);
        	isAppInstalled = true;
        }
    }
    
    public String[] getInstalledApkVersion(String packageName) {
        //adb -s UDID shell dumpsys package PACKAGE | grep versionCode
        if (isNull())
        	return null;

        String[] res = new String[3];
        res[0] = packageName;

        String[] cmd = CmdLine.insertCommandsAfter(executor.getDefaultCmd(), "-s", getAdbName(), "shell", "dumpsys", "package", packageName);
        List<String> output = executor.execute(cmd);


        for (String line : output) {
            LOGGER.debug(line);
            if (line.contains("versionCode")) {
                // versionCode=17040000 targetSdk=25
                LOGGER.info("Line for parsing installed app: " + line);
                String[] outputs = line.split("=");
                String tmp = outputs[1]; //everything after '=' sign
                res[1] = tmp.split(" ")[0];
            }

            if (line.contains("versionName")) {
                // versionName=8.5.0
                LOGGER.info("Line for parsing installed app: " + line);
                String[] outputs = line.split("=");
                res[2] = outputs[1];
            }
        }

        if (res[0] == null && res[1] == null && res[2] == null) {
        	return null;
        }
        return res;
    }
    
    public String[] getApkVersion(String apkFile) {
        // aapt dump badging <apk_file> | grep versionCode
        // aapt dump badging <apk_file> | grep versionName
        // output:
        // package: name='com.myfitnesspal.android' versionCode='9025' versionName='develop-QA' platformBuildVersionName='6.0-2704002'

        String[] res = new String[3];
        res[0] = "";
        res[1] = "";
        res[2] = "";
        
        String[] cmd = CmdLine.insertCommandsAfter("aapt dump badging".split(" "), apkFile);
        List<String> output = executor.execute(cmd);
        // parse output command and get appropriate data


        for (String line : output) {
            if (line.contains("versionCode") && line.contains("versionName")) {
                LOGGER.debug(line);
                String[] outputs = line.split("'");
                res[0] = outputs[1]; //package
                res[1] = outputs[3]; //versionCode
                res[2] = outputs[5]; //versionName
            }
        }

        return res;
    }

    
    public void restartAppium() {
        if (!Configuration.getBoolean(Parameter.MOBILE_APPIUM_RESTART))
            return;
        
        if (isNull())
        	return;

        stopAppium();
        startAppium();
    }

    // TODO: think about moving shutdown/startup scripts into external property and make it cross platform 
    public void stopAppium() {
        if (!Configuration.getBoolean(Parameter.MOBILE_APPIUM_RESTART))
            return;
        
        if (isNull())
        	return;

        LOGGER.info("Stopping appium...");

        String cmdLine = Configuration.get(Parameter.MOBILE_TOOLS_HOME) + "/stopNodeAppium.sh";
        String[] cmd = CmdLine.insertCommandsAfter(cmdLine.split(" "), getUdid());
        List<String> output = executor.execute(cmd);
        for (String line : output) {
            LOGGER.debug(line);
        }
    }

    public void startAppium() {
        if (!Configuration.getBoolean(Parameter.MOBILE_APPIUM_RESTART))
            return;
        
        if (isNull())
        	return;

        LOGGER.info("Starting appium...");

        String cmdLine = Configuration.get(Parameter.MOBILE_TOOLS_HOME) + "/startNodeAppium.sh";
        String[] cmd = CmdLine.insertCommandsAfter(cmdLine.split(" "), getUdid(), "&");
        List<String> output = executor.execute(cmd);
        for (String line : output) {
            LOGGER.debug(line);
        }

        AppiumStatus.waitStartup(getSeleniumServer(), 30);
    }
    
    public List<String> execute(String[] cmd) {
    	return executor.execute(cmd);
    }
    
    public void setProxy(final String host, final String port, final String ssid, final String password) {
        if (!getOs().equalsIgnoreCase(DeviceType.Type.ANDROID_PHONE.getFamily())) {
            LOGGER.error("Proxy configuration is available for Android ONLY");
            throw new RuntimeException("Proxy configuration is available for Android ONLY");
        }
        if (!isAppInstall(SpecialKeywords.PROXY_SETTER_PACKAGE)) {
            final String proxySetterFileName = "./proxy-setter-temp.apk";
            File targetFile = new File(proxySetterFileName);
            downloadFileFromJar(SpecialKeywords.PROXY_SETTER_RES_PATH, targetFile);
            installApp(proxySetterFileName);
        }
        String deviceUdid = getAdbName();
        LOGGER.info("Device udid: ".concat(deviceUdid));
        String[] cmd = CmdLine.createPlatformDependentCommandLine("adb", "-s", deviceUdid, "shell", "am", "start", "-n",
                "tk.elevenk.proxysetter/.MainActivity", "-e", "host", host, "-e", "port", port, "-e", "ssid", ssid, "-e", "key", password);
        LOGGER.info("Following cmd will be executed: " + Arrays.toString(cmd));
        executor.execute(cmd);
    }

    private void downloadFileFromJar(final String path, final File targetFile) {
        InputStream initialStream = Device.class.getClassLoader().getResourceAsStream(path);
        try {
            FileUtils.copyInputStreamToFile(initialStream, targetFile);
        } catch (IOException e) {
            LOGGER.error("Error during copying of file from the resources. ".concat(e.getMessage()));
        }
    }
    
    public String getAdbName() {
    	if (remoteURL != null) {
    		return remoteURL;
    	} else {
    		return udid;
    	}
    }

}
