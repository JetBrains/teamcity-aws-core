/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.util.amazon;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.codebuild.AWSCodeBuildClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codepipeline.AWSCodePipelineClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public class AWSClients {

  @Nullable private final AWSCredentials myCredentials;
  @Nullable private String myServiceEndpoint;
  @Nullable private String myS3SignerType;
  @NotNull private final String myRegion;
  @NotNull private final ClientConfiguration myClientConfiguration;

  private AWSClients(@Nullable AWSCredentials credentials, @NotNull String region) {
    myCredentials = credentials;
    myRegion = region;
    myClientConfiguration = AWSCommonParams.createClientConfiguration();
  }

  @Nullable
  public AWSCredentials getCredentials() {
    return myCredentials;
  }

  @NotNull
  public ClientConfiguration getClientConfiguration() {
    return myClientConfiguration;
  }

  @NotNull
  public static AWSClients fromDefaultCredentialProviderChain(@NotNull String region) {
    return fromExistingCredentials(null, region);
  }
  @NotNull
  public static AWSClients fromBasicCredentials(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String region) {
    return fromExistingCredentials(new BasicAWSCredentials(accessKeyId, secretAccessKey), region);
  }

  @NotNull
  public static AWSClients fromBasicSessionCredentials(@NotNull String accessKeyId, @NotNull String secretAccessKey, @NotNull String sessionToken, @NotNull String region) {
    return fromExistingCredentials(new BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken), region);
  }

  @NotNull
  public static AWSClients fromSessionCredentials(@NotNull final String accessKeyId, @NotNull final String secretAccessKey,
                                                  @NotNull final String iamRoleARN, @Nullable final String externalID,
                                                  @NotNull final String sessionName, final int sessionDuration,
                                                  @NotNull final String region) {
    return fromExistingCredentials(new AWSCommonParams.LazyCredentials() {
      @NotNull
      @Override
      protected AWSSessionCredentials createCredentials() {
        return AWSClients.fromBasicCredentials(accessKeyId, secretAccessKey, region).createSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration);
      }
    }, region);
  }

  @NotNull
  public static AWSClients fromSessionCredentials(@NotNull final String iamRoleARN, @Nullable final String externalID,
                                                  @NotNull final String sessionName, final int sessionDuration,
                                                  @NotNull final String region) {
    return fromExistingCredentials(new AWSCommonParams.LazyCredentials() {
      @NotNull
      @Override
      protected AWSSessionCredentials createCredentials() {
        return AWSClients.fromDefaultCredentialProviderChain(region).createSessionCredentials(iamRoleARN, externalID, sessionName, sessionDuration);
      }
    }, region);
  }

  @NotNull
  private static AWSClients fromExistingCredentials(@Nullable AWSCredentials credentials, @NotNull String region) {
    return new AWSClients(credentials, region);
  }

  @NotNull
  public AmazonS3 createS3Client() {
    if (StringUtil.isNotEmpty(myS3SignerType)) {
      myClientConfiguration.setSignerOverride(myS3SignerType);
    }

    final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
            .withClientConfiguration(myClientConfiguration)
            .withPathStyleAccessEnabled(true);

    if (myCredentials != null) {
      builder.setCredentials(new AWSStaticCredentialsProvider(myCredentials));
    }

    if (StringUtil.isNotEmpty(myServiceEndpoint)) {
      builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(myServiceEndpoint, myRegion));
    } else {
      builder.withRegion(myRegion);
    }

    return builder.build();
  }

  @NotNull
  public AmazonCodeDeployClient createCodeDeployClient() {
    return withRegion(myCredentials == null ? new AmazonCodeDeployClient(myClientConfiguration) : new AmazonCodeDeployClient(myCredentials, myClientConfiguration));
  }

  @NotNull
  public AWSCodePipelineClient createCodePipeLineClient() {
    return withRegion(myCredentials == null ? new AWSCodePipelineClient(myClientConfiguration) : new AWSCodePipelineClient(myCredentials, myClientConfiguration));
  }

  @NotNull
  public AWSCodeBuildClient createCodeBuildClient() {
    return withRegion(myCredentials == null ? new AWSCodeBuildClient(myClientConfiguration) : new AWSCodeBuildClient(myCredentials, myClientConfiguration));
  }

  @NotNull
  private AWSSecurityTokenServiceClient createSecurityTokenServiceClient() {
    return myCredentials == null ? new AWSSecurityTokenServiceClient(myClientConfiguration) : new AWSSecurityTokenServiceClient(myCredentials, myClientConfiguration);
  }

  @NotNull
  public String getRegion() {
    return myRegion;
  }

  public void setServiceEndpoint(@NotNull final String serviceEndpoint) {
    myServiceEndpoint = StringUtil.trimEnd(serviceEndpoint,"/");
  }

  public void setS3SignerType(@NotNull final String s3SignerType) {
    myS3SignerType = s3SignerType;
  }

  @NotNull
  private <T extends AmazonWebServiceClient> T withRegion(@NotNull T client) {
    return client.withRegion(AWSRegions.getRegion(myRegion));
  }

  @NotNull
  private AWSSessionCredentials createSessionCredentials(@NotNull String iamRoleARN, @Nullable String externalID, @NotNull String sessionName, int sessionDuration) throws AWSException {
    final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest().withRoleArn(iamRoleARN).withRoleSessionName(AWSCommonParams.patchSessionName(sessionName)).withDurationSeconds(AWSCommonParams.patchSessionDuration(sessionDuration));
    if (StringUtil.isNotEmpty(externalID)) assumeRoleRequest.setExternalId(externalID);
    try {
      final Credentials credentials = createSecurityTokenServiceClient().assumeRole(assumeRoleRequest).getCredentials();
      return new BasicSessionCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSessionToken());
    } catch (Exception e) {
      throw new AWSException(e);
    }
  }

  public static final String UNSUPPORTED_SESSION_NAME_CHARS = "[^\\w+=,.@-]";
  public static final int MAX_SESSION_NAME_LENGTH = 64;

}
