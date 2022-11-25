/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
 */

package jetbrains.buildServer.clouds.amazon.connector.impl.defaultProviderType;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import java.util.Date;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.errors.AwsConnectorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultProviderCredentialsHolder implements AwsCredentialsHolder {

  private final AWSCredentials credentials;

  public DefaultProviderCredentialsHolder() throws AwsConnectorException {
    try {
      credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
    } catch (Exception e) {
      throw new AwsConnectorException("Failed to use the DefaultAWSCredentialsProviderChain, reason: " + e.getMessage(), e);
    }
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() {
    return new AwsCredentialsData() {
      @NotNull
      @Override
      public String getAccessKeyId() {
        return credentials.getAWSAccessKeyId();
      }

      @NotNull
      @Override
      public String getSecretAccessKey() {
        return credentials.getAWSSecretKey();
      }

      @Nullable
      @Override
      public String getSessionToken() {
        if (credentials instanceof AWSSessionCredentials) {
          return ((AWSSessionCredentials)credentials).getSessionToken();
        } else {
          return null;
        }
      }
    };
  }

  @Override
  public void refreshCredentials() {
    //...
  }

  @Nullable
  @Override
  public Date getSessionExpirationDate() {
    return null;
  }
}
