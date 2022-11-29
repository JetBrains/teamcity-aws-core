package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.impl.BaseAwsCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class StaticCredentialsBuilder extends BaseAwsCredentialsBuilder {

  public StaticCredentialsBuilder(@NotNull final AwsConnectorFactory awsConnectorFactory) {
    awsConnectorFactory.registerAwsCredentialsBuilder(this);
  }

  @NotNull
  @Override
  protected AwsCredentialsHolder constructSpecificCredentialsProviderImpl(@NotNull final SProjectFeatureDescriptor featureDescriptor) {
    Map<String, String> cloudConnectorProperties = featureDescriptor.getParameters();
    if (ParamUtil.useSessionCredentials(cloudConnectorProperties)) {
      Loggers.CLOUD.debug("Using Session credentials for the AWS key: " + ParamUtil.maskKey(cloudConnectorProperties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM)));
      return createSessionCredentialsHolder(cloudConnectorProperties);
    } else {
      return getBasicCredentialsProvider(cloudConnectorProperties);
    }
  }

  @Override
  @NotNull
  public List<InvalidProperty> validateProperties(@NotNull final Map<String, String> properties) {
    List<InvalidProperty> invalidProperties = new ArrayList<>();

    if (StringUtil.isEmpty(properties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM, "Please provide the access key ID"));
    }
    if (StringUtil.isEmpty(properties.get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, "Please provide the secret access key "));
    }

    if (StringUtil.isEmpty(StringUtil.emptyIfNull(properties.get(AwsCloudConnectorConstants.REGION_NAME_PARAM)))) {
      invalidProperties.add(new InvalidProperty(AwsCloudConnectorConstants.REGION_NAME_PARAM, "Please choose the region where this AWS Connection will be used"));
    }

    if (!ParamUtil.isValidSessionDuration(properties.get(AwsSessionCredentialsParams.SESSION_DURATION_PARAM))) {
      invalidProperties.add(new InvalidProperty(AwsSessionCredentialsParams.SESSION_DURATION_PARAM, "Session duration is not valid"));
    }

    if (!StsEndpointParamValidator.isValidStsEndpoint(properties.get(AwsAccessKeysParams.STS_ENDPOINT_PARAM))) {
      invalidProperties.add(
        new InvalidProperty(AwsAccessKeysParams.STS_ENDPOINT_PARAM, "The STS endpoint is not a valid URL, please, provide a valid URL"));
    }

    return invalidProperties;
  }

  @Override
  @NotNull
  public String getCredentialsType() {
    return AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE;
  }

  @Override
  @NotNull
  public String getPropertiesDescription(@NotNull final Map<String, String> properties) {
    return "Static IAM Access Key";
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultProperties() {
    return Collections.singletonMap(AwsAccessKeysParams.SESSION_CREDENTIALS_PARAM, AwsAccessKeysParams.SESSION_CREDENTIALS_DEFAULT);
  }

  @NotNull
  protected AwsCredentialsHolder createSessionCredentialsHolder(@NotNull final Map<String, String> cloudConnectorProperties) {
    return new StaticSessionCredentialsHolder(
      getBasicCredentialsProvider(cloudConnectorProperties),
      cloudConnectorProperties
    );
  }

  @NotNull
  private AwsCredentialsHolder getBasicCredentialsProvider(@NotNull final Map<String, String> cloudConnectorProperties) {
    return new StaticCredentialsHolder(cloudConnectorProperties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM),
                                       cloudConnectorProperties.get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM));
  }
}
