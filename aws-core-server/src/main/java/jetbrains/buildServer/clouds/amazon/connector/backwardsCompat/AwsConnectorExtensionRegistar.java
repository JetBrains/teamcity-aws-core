package jetbrains.buildServer.clouds.amazon.connector.backwardsCompat;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

public class AwsConnectorExtensionRegistar {
  public AwsConnectorExtensionRegistar(@NotNull final ExtensionHolder extensionHolder,
                                       @NotNull final PluginDescriptor pluginDescriptor,
                                       @NotNull final AwsConnectorFactory awsConnectorFactory) {
    if (TeamCityProperties.getBoolean(AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME)) {
      extensionHolder.registerExtension(OAuthProvider.class, AwsConnectionProvider.class.getName(), new AwsConnectionProvider(pluginDescriptor, awsConnectorFactory));
      Loggers.CLOUD.info("AWS Core plugin is loaded.");
    } else {
      Loggers.CLOUD.debug("AWS Core plugin has not been loaded since the is no definied internal property or its turned off: " + AwsCloudConnectorConstants.FEATURE_PROPERTY_NAME);
    }
  }
}
