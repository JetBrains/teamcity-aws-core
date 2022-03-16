package jetbrains.buildServer.clouds.amazon.connector.utils.clients;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.util.StringUtil;

public class StsClientBuilder {

  public static AWSSecurityTokenService buildClient(Map<String, String> properties) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    addConfiguration(stsBuilder, properties);
    return stsBuilder.build();
  }

  public static AWSSecurityTokenService buildClientWithCredentials(Map<String, String> properties) {
    AWSSecurityTokenServiceClientBuilder stsBuilder = AWSSecurityTokenServiceClientBuilder.standard();
    addConfiguration(stsBuilder, properties);

    String accessKey = StringUtil.emptyIfNull(properties.get(AwsAccessKeysParams.ACCESS_KEY_ID_PARAM));
    String secretKey = StringUtil.emptyIfNull(properties.get(AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM));
    stsBuilder.withCredentials(
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey)
      )
    );

    return stsBuilder.build();
  }

  private static void addConfiguration(AWSSecurityTokenServiceClientBuilder stsBuilder, Map<String, String> properties) {
    String stsEndpoint = StringUtil.emptyIfNull(properties.get(AwsAccessKeysParams.STS_ENDPOINT_PARAM));
    if (!stsEndpoint.equals(AwsAccessKeysParams.STS_ENDPOINT_DEFAULT)) {

      AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
        stsEndpoint,
        Regions.US_EAST_1.getName()
      );

      stsBuilder.withEndpointConfiguration(endpointConfiguration);
    }

    stsBuilder.withClientConfiguration(ClientConfigurationBuilder.createClientConfigurationEx("sts"));
  }
}
