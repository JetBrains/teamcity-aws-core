package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions.AWSRegions;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StsEndpointParamValidator {
  public static final String STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME = "teamcity.aws.connection.stsEndpointsAllowlist";
  public static final String REGION_TO_STS_ENDPOINT_FORMAT = "https://sts.%s.amazonaws.com";
  public static final String GLOBAL_AWS_STS_ENDPOINT = "https://sts.amazonaws.com";
  public static final String STS_ENDPOINTS_ALLOWLIST_SEPARATOR = ",";

  public static boolean isValidStsEndpoint(@Nullable final String url) {
    if (url == null) {
      return false;
    }

    return getStsEndpoints().contains(url);
  }

  public static List<String> getStsEndpoints() {
    List<String> res = new ArrayList<>();
    String allowedStsEndpointsProperty = TeamCityProperties.getPropertyOrNull(STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME);

    if (StringUtil.nullIfEmpty(allowedStsEndpointsProperty) != null) {
      res = parseAllowedStsEndpoints(allowedStsEndpointsProperty);
    } else {
      for (String regionName : AWSRegions.getAllRegions().keySet()) {
        res.add(String.format(REGION_TO_STS_ENDPOINT_FORMAT, regionName));
      }
      res.add(GLOBAL_AWS_STS_ENDPOINT);
    }
    return res;
  }

  private static List<String> parseAllowedStsEndpoints(@NotNull final String allowedStsEndpointsString) {
    return Arrays.stream(
      allowedStsEndpointsString
        .split(STS_ENDPOINTS_ALLOWLIST_SEPARATOR)
    ).collect(Collectors.toList());
  }
}
