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
package com.qaprosoft.appcenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.qaprosoft.appcenter.http.resttemplate.RestTemplateBuilder;
import com.qaprosoft.carina.core.foundation.utils.Configuration;
import com.qaprosoft.carina.core.foundation.utils.Configuration.Parameter;

/**
 * Created by boyle on 8/16/17.
 */
public class AppCenterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    protected RestTemplate restTemplate = RestTemplateBuilder.newInstance().withDisabledSslChecking().withSpecificJsonMessageConverter().build();

    private String ownerName;
    private String versionLong;
    private String versionShort;

    private static final String HOST_URL = "api.appcenter.ms";
    private static final String API_APPS = "/v0.1/apps";

    private static AppCenterManager instance = null;

    private AppCenterManager() {
    }

    public synchronized static AppCenterManager getInstance() {
        if (instance == null) {
            instance = new AppCenterManager();
        }
        return instance;
    }

    /**
    *
    * @param appName takes in the AppCenter Name to look for.
    * @param platformName takes in the platform we wish to download for.
    * @param buildType takes in the particular build to download (i.e. Prod.AdHoc, QA.Debug, Prod-Release, QA-Internal etc...)
    * @param version takes in either "latest" to take the first build that matches the criteria or allows to consume a version to download that
    *            build.
    * @return download url for build artifact.
    */
   public String getDownloadUrl(String appName, String platformName, String buildType, String version) {
       LOGGER.info("appName: " + appName + "; version: " + version);
       return scanAppForBuild(getAppId(appName, platformName), buildType, version);
   }    

    /**
     *
     * @param folder to which upload build artifact.
     * @param appName takes in the AppCenter Name to look for.
     * @param platformName takes in the platform we wish to download for.
     * @param buildType takes in the particular build to download (i.e. Prod.AdHoc, QA.Debug, Prod-Release, QA-Internal etc...)
     * @param version takes in either "latest" to take the first build that matches the criteria or allows to consume a version to download that
     *            build.
     * @return file to the downloaded build artifact
     */
    public File getBuild(String folder, String appName, String platformName, String buildType, String version) {
        String buildToDownload = getDownloadUrl(appName, platformName, buildType, version);

        //TODO: wrap below code into the public download method
        String fileName = folder + "/" + createFileName(appName, buildType, platformName);
        File fileToLocate = null;

        try {
            File file = new File(folder);
            File[] listOfFiles = file.listFiles();

            if (file.list() != null) {
                for (int i = 0; i < listOfFiles.length; ++i) {
                    if (listOfFiles[i].isFile() && fileName.contains(listOfFiles[i].getName())) {
                        LOGGER.info("File has been Located Locally.  File path is: " + listOfFiles[i].getAbsolutePath());
                        fileToLocate = listOfFiles[i];
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("Error Attempting to Look for Existing File: %s", ex.getMessage()), ex);
        }

        if (fileToLocate == null) {
            try {
                LOGGER.debug("Beginning Transfer of AppCenter Build");
                URL downloadLink = new URL(buildToDownload);
                int retryCount = 0;
                boolean retry = true;
                while (retry && retryCount <= 5) {
                    retry = downloadBuild(fileName, downloadLink);
                    retryCount = retryCount + 1;
                }
                LOGGER.debug(String.format("AppCenter Build (%s) was retrieved", fileName));
            } catch (Exception ex) {
                LOGGER.error(String.format("Error Thrown When Attempting to Transfer AppCenter Build (%s)", ex.getMessage()), ex);
            }
        } else {
            LOGGER.info("Preparing to use local version of AppCenter Build...");
        }

        return new File(fileName);
    }

    /**
     *
     * @param fileName will be the name of the downloaded file.
     * @param downloadLink will be the URL to retrieve the build from.
     * @return brings back a true/false on whether or not the build was successfully downloaded.
     * @throws IOException throws a non Interruption Exception up.
     */
    private boolean downloadBuild(String fileName, URL downloadLink) throws IOException {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(downloadLink.openStream());
                FileOutputStream fos = new FileOutputStream(fileName)) {
            if (Thread.currentThread().isInterrupted()) {
                LOGGER.debug(String.format("Current Thread (%s) is interrupted, clearing interruption.", Thread.currentThread().getId()));
                Thread.interrupted();
            }
            fos.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            LOGGER.info("Successfully Transferred...");
            return false;
        } catch (ClosedByInterruptException ie1) {
            LOGGER.info("Retrying....");
            LOGGER.error("Getting Error: " + ie1.getMessage(), ie1);
            return true;
        }
    }

    /**
     *
     * @param appName takes in the AppCenter Name to look for.
     * @param platformName takes in the platform we wish to download for.
     * @return
     */
    private Map<String, String> getAppId(String appName, String platformName) {
        LOGGER.info("appName: " + appName + "; platformName: " + platformName);
        Map<String, String> appMap = new HashMap<>();

        RequestEntity<String> retrieveApps = buildRequestEntity(
                HOST_URL,
                API_APPS,
                HttpMethod.GET);
        LOGGER.info("before rest call");
        JsonNode appResults = this.restTemplate.exchange(retrieveApps, JsonNode.class).getBody();
        LOGGER.info("after rest call, appResults: " + appResults.toString());
        LOGGER.info("AppCenter Searching For App: " + appName);
        LOGGER.debug("AppCenter JSON Response: " + appResults);

        for (JsonNode node : appResults) {
            if (platformName.equalsIgnoreCase(node.get("os").asText()) && node.get("name").asText().toLowerCase().contains(appName.toLowerCase())) {
                ownerName = node.get("owner").get("name").asText();
                String app = node.get("name").asText();
                LOGGER.info(String.format("Found Owner: %s App: %s", ownerName, app));
                appMap.put(app, getLatestBuildDate(app, node.get("updated_at").asText()));
            }
        }

        if (!appMap.isEmpty()) {
            return appMap.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        throw new RuntimeException(String.format("Application Not Found in AppCenter for Organization (%s) Name (%s), Platform (%s)", ownerName, appName, platformName));
    }

    /**
     *
     * @param apps takes in the application Ids
     * @param buildType takes in the particular build to download (i.e. Prod.AdHoc, QA.Debug, Prod-Release, QA-Internal etc...)
     * @param version takes in either "latest" to take the first build that matches the criteria or allows to consume a version to download that
     *            build.
     * @return
     */
    private String scanAppForBuild(Map<String, String> apps, String buildType, String version) {
        for (String currentApp : apps.keySet()) {
            LOGGER.info("Scanning App " + currentApp);
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("published_only", "true");

            RequestEntity<String> retrieveList = buildRequestEntity(
                    HOST_URL,
                    String.format("%s/%s/%s/releases", API_APPS, ownerName, currentApp),
                    queryParams,
                    HttpMethod.GET);
            JsonNode buildList = restTemplate.exchange(retrieveList, JsonNode.class).getBody();
            LOGGER.debug("Available Builds JSON: " + buildList);

            if (buildList.size() > 0) {
                int buildLimiter = 0;
                for (JsonNode build : buildList) {

                    buildLimiter += 1;
                    if (buildLimiter >=50) {
                        break;
                    }

                    String latestBuildNumber = build.get("id").asText();
                    versionShort = build.get("short_version").asText();
                    versionLong = build.get("version").asText();

                    RequestEntity<String> retrieveBuildUrl = buildRequestEntity(
                            HOST_URL,
                            String.format("%s/%s/%s/releases/%s", API_APPS, ownerName, currentApp, latestBuildNumber),
                            HttpMethod.GET);
                    JsonNode appBuild = restTemplate.exchange(retrieveBuildUrl, JsonNode.class).getBody();
                    if (checkBuild(version, appBuild) && (checkTitleForCorrectPattern(buildType.toLowerCase(), appBuild) || checkNotesForCorrectBuild(buildType.toLowerCase(), appBuild))) {
                        LOGGER.debug("Print Build Info: " + appBuild);
                        LOGGER.info(
                                String.format(
                                        "Fetching Build ID (%s) Version: %s (%s)", latestBuildNumber, versionShort, versionLong));
                        String buildUrl = appBuild.get("download_url").asText();
                        LOGGER.info("Download URL For Build: " + buildUrl);

                        return buildUrl;
                    }
                }
            }
        }

        throw new RuntimeException(String.format("Unable to find build to download, version provided (%s)", version));
    }

    /**
     * The updated_at field returned by AppCenter doesn't contain the "latest time" a build was updated, so we grab the first build to do our sort.
     * @param app name of the app to check.
     * @param appUpdatedAt passing in of a backup date value if the app we look at doesn't have a build associated to it.
     * @return the date value to be used in sorting.
     */
    private String getLatestBuildDate(String app, String appUpdatedAt) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("published_only", "true");

        RequestEntity<String> retrieveList = buildRequestEntity(
                HOST_URL,
                String.format("%s/%s/%s/releases", API_APPS, ownerName, app),
                queryParams,
                HttpMethod.GET);
        JsonNode buildList = restTemplate.exchange(retrieveList, JsonNode.class).getBody();
        if (buildList.size() > 0) {
            return buildList.get(0).get("uploaded_at").asText();
        }
        return appUpdatedAt;
    }

    private boolean checkBuild(String version, JsonNode node) {

        if ("latest".equalsIgnoreCase(version)) {
            return true;
        }

        return version.equalsIgnoreCase(
                node.get("short_version").asText() + "." + node.get("version").asText())
                || version.equalsIgnoreCase(node.get("short_version").asText());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private RequestEntity<String> buildRequestEntity(String hostUrl, String path,
                                                     HttpMethod httpMethod) {

        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(hostUrl)
                .path(path)
                .build();

        return new RequestEntity(setHeaders(), httpMethod, uriComponents.toUri());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private RequestEntity<String> buildRequestEntity(String hostUrl, String path,
                                                     MultiValueMap<String, String> listQueryParams, HttpMethod httpMethod) {

        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(hostUrl)
                .path(path)
                .queryParams(listQueryParams)
                .build();

        return new RequestEntity(setHeaders(), httpMethod, uriComponents.toUri());
    }

    private HttpHeaders setHeaders() {

        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.add("Content-Type", "application/json; charset=utf-8");
        httpHeader.add("x-api-token", Configuration.get(Parameter.APPCENTER_TOKEN));

        return httpHeader;
    }

    private String createFileName(String appName, String buildType, String platformName) {

        String fileName = String.format("%s.%s.%s.%s", appName, buildType, versionShort, versionLong)
                .replace(" ", "");

        if (platformName.toLowerCase().contains("ios")) {
            return fileName + ".ipa";
        }
        return fileName + ".apk";
    }

    private boolean checkNotesForCorrectBuild(String pattern, JsonNode node) {

        return checkForPattern("release_notes", pattern, node);
    }

    private boolean checkTitleForCorrectPattern(String pattern, JsonNode node) {
        return checkForPattern("app_name", pattern, node);
    }

    private boolean checkForPattern(String nodeName, String pattern, JsonNode node) {
        LOGGER.debug("\nPattern to be checked: " + pattern);
        if (node.findPath("release_notes").isMissingNode()) {
            return false;
        }

        String nodeField = node.get(nodeName).asText().toLowerCase();
        String[] splitPattern = pattern.split("\\.");
        LinkedList<Boolean> segmentsFound = new LinkedList<>();
        for(String segment : splitPattern){
            segmentsFound.add(nodeField.contains(segment));
        }

        if (!segmentsFound.isEmpty() && !segmentsFound.contains(false)) {
            LOGGER.debug("\nPattern match found!! This is the buildType to be used: " + nodeField);
            return true;
        }
        String patternToReplace = ".*[ ->\\S]%s[ -<\\S].*";
        return !pattern.isEmpty() && scanningAllNotes(String.format(patternToReplace, pattern), nodeField);
    }

    private boolean searchFieldsForString(String pattern, String stringToSearch) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(stringToSearch);

        return m.find();
    }

    private boolean scanningAllNotes(String pattern, String noteField) {
        boolean foundMessages = false;

        foundMessages = searchFieldsForString(pattern, noteField);

        return foundMessages;
    }
}