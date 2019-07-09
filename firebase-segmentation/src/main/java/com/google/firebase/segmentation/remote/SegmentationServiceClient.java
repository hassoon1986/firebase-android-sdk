// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.segmentation.remote;

import androidx.annotation.NonNull;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

/** Http client that sends request to Firebase Segmentation backend API. To be implemented */
public class SegmentationServiceClient {

  private static final String FIREBASE_SEGMENTATION_API_DOMAIN =
      "firebasesegmentation.googleapis.com";
  private static final String UPDATE_REQUEST_RESOURCE_NAME_FORMAT =
      "projects/%s/installations/%s/customSegmentationData";
  private static final String CLEAR_REQUEST_RESOURCE_NAME_FORMAT =
      "projects/%s/installations/%s/customSegmentationData:clear";
  private static final String FIREBASE_SEGMENTATION_API_VERSION = "v1alpha";

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient;

  public enum Code {
    OK,

    HTTP_CLIENT_ERROR,

    CONFLICT,

    NETWORK_ERROR,

    SERVER_ERROR,

    UNAUTHORIZED,
  }

  public SegmentationServiceClient() {
    httpClient = new OkHttpClient();
  }

  @NonNull
  public Code updateCustomInstallationId(
      long projectNumber,
      @NonNull String apiKey,
      @NonNull String customInstallationId,
      @NonNull String firebaseInstanceId,
      @NonNull String firebaseInstanceIdToken) {
    String resourceName =
        String.format(UPDATE_REQUEST_RESOURCE_NAME_FORMAT, projectNumber, firebaseInstanceId);

    RequestBody requestBody;
    try {
      requestBody =
          RequestBody.create(
              JSON,
              buildUpdateCustomSegmentationDataRequestBody(resourceName, customInstallationId)
                  .toString());
    } catch (JSONException e) {
      // This should never happen
      throw new IllegalStateException(e);
    }

    Request request =
        new Request.Builder()
            .url(
                String.format(
                    "https://%s/%s/%s?key=%s",
                    FIREBASE_SEGMENTATION_API_DOMAIN,
                    FIREBASE_SEGMENTATION_API_VERSION,
                    resourceName,
                    apiKey))
            .header("Authorization", "FIREBASE_INSTALLATIONS_AUTH " + firebaseInstanceIdToken)
            .header("Content-Type", "application/json")
            .patch(requestBody)
            .build();

    try {
      Response response = httpClient.newCall(request).execute();
      switch (response.code()) {
        case 200:
          return Code.OK;
        case 401:
          return Code.UNAUTHORIZED;
        case 409:
          return Code.CONFLICT;
        default:
          return Code.SERVER_ERROR;
      }
    } catch (IOException e) {
      return Code.NETWORK_ERROR;
    }
  }

  private static JSONObject buildUpdateCustomSegmentationDataRequestBody(
      String resourceName, String customInstallationId) throws JSONException {
    JSONObject customSegmentationData = new JSONObject();
    customSegmentationData.put("name", resourceName);
    customSegmentationData.put("custom_installation_id", customInstallationId);
    return customSegmentationData;
  }

  @NonNull
  public Code clearCustomInstallationId(
      long projectNumber,
      @NonNull String apiKey,
      @NonNull String firebaseInstanceId,
      @NonNull String firebaseInstanceIdToken) {
    String resourceName =
        String.format(CLEAR_REQUEST_RESOURCE_NAME_FORMAT, projectNumber, firebaseInstanceId);

    RequestBody requestBody;
    try {
      requestBody =
          RequestBody.create(
              JSON, buildClearCustomSegmentationDataRequestBody(resourceName).toString());
    } catch (JSONException e) {
      // This should never happen
      throw new IllegalStateException(e);
    }

    Request request =
        new Request.Builder()
            .url(
                String.format(
                    "https://%s/%s/%s?key=%s",
                    FIREBASE_SEGMENTATION_API_DOMAIN,
                    FIREBASE_SEGMENTATION_API_VERSION,
                    resourceName,
                    apiKey))
            .header("Authorization", "FIREBASE_INSTALLATIONS_AUTH " + firebaseInstanceIdToken)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build();

    try {
      Response response = httpClient.newCall(request).execute();
      switch (response.code()) {
        case 200:
          return Code.OK;
        case 401:
          return Code.UNAUTHORIZED;
        default:
          return Code.SERVER_ERROR;
      }
    } catch (IOException e) {
      return Code.NETWORK_ERROR;
    }
  }

  private static JSONObject buildClearCustomSegmentationDataRequestBody(String resourceName)
      throws JSONException {
    return new JSONObject().put("name", resourceName);
  }
}
