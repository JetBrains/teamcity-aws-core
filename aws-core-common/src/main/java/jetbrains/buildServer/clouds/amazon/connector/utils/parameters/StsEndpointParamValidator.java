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

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions.AWSRegions.isChinaRegion;

public class StsEndpointParamValidator {
  public static final String WHITELISTED_STS_ENDPOINTS_PROPERTY_NAME = "teamcity.aws.connection.whitelistedStsEndpoints";
  public static final String REGION_TO_STS_ENDPOINT_FORMAT = "https://sts.%s.amazonaws.com";
  public static final String CHINA_REGION_TO_STS_ENDPOINT_FORMAT = "https://sts.%s.amazonaws.com.cn";
  public static final String GLOBAL_AWS_STS_ENDPOINT = "https://sts.amazonaws.com";
  public static final String WHITELISTED_STS_ENDPOINTS_SEPARATOR = ",";

  public static boolean isValidStsEndpoint(@Nullable final String url) {
    if (url == null) {
      return false;
    }

    return getStsEndpoints().contains(url);
  }

  public static List<String> getStsEndpoints() {
    List<String> res = new ArrayList<>();
    String whiteListedStsEndpointsProperty = TeamCityProperties.getPropertyOrNull(WHITELISTED_STS_ENDPOINTS_PROPERTY_NAME);

    if (StringUtil.nullIfEmpty(whiteListedStsEndpointsProperty) != null) {
      res = parseWhitelistedStsEndpoints(whiteListedStsEndpointsProperty);
    } else {
      for (String regionName : AWSRegions.getAllRegions().keySet()) {
        if (isChinaRegion(regionName)) {
          res.add(String.format(CHINA_REGION_TO_STS_ENDPOINT_FORMAT, regionName));
        } else {
          res.add(String.format(REGION_TO_STS_ENDPOINT_FORMAT, regionName));
        }
      }
      res.add(GLOBAL_AWS_STS_ENDPOINT);
    }
    return res;
  }

  private static List<String> parseWhitelistedStsEndpoints(@NotNull final String whiteListedStsEndpointsString) {
    return Arrays.stream(
      whiteListedStsEndpointsString
        .split(WHITELISTED_STS_ENDPOINTS_SEPARATOR)
    ).collect(Collectors.toList());
  }
}
