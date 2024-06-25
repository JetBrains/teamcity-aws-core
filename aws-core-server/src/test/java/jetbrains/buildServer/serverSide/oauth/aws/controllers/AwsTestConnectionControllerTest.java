package jetbrains.buildServer.serverSide.oauth.aws.controllers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.AwsConnectionTester;
import jetbrains.buildServer.clouds.amazon.connector.connectionTesting.impl.AwsTestConnectionResult;
import jetbrains.buildServer.clouds.amazon.connector.impl.AwsConnectorFactoryImpl;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsSessionCredentialsParams;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.StsEndpointParamValidator;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionCredentialsFactory;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.ProjectFeatureDescriptorImpl;
import jetbrains.buildServer.testUtils.AbstractControllerTest;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAccessKeysParams.*;
import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.*;
import static jetbrains.buildServer.testUtils.TestUtils.getStsClientProvider;
import static org.mockito.Mockito.when;

public class AwsTestConnectionControllerTest extends AbstractControllerTest {

  private final String testAccessKeyId = "TEST_ACCESS";
  private final String testSecretAccessKey = "TEST_SECRET";

  private final String testAccountId = "TEST ACCOUNT";
  private final String testArn = "TEST ARN";
  private final String testUserId = "TEST USER";
  private final String propertyPrefix = "prop:";

  private AwsConnectorFactory myAwsConnectorFactory;
  private AwsTestConnectionController myAwsTestConnectionController;

  private Element xmlResponse;

  @Override
  @BeforeMethod
  public void setUp() throws IOException {
    super.setUp();
    myAwsConnectorFactory = new AwsConnectorFactoryImpl();
    try {
      new StaticCredentialsBuilder(
        myAwsConnectorFactory,
        Mockito.mock(AwsConnectionCredentialsFactory.class),
        getStsClientProvider(testAccessKeyId, testSecretAccessKey, null)
      );
    } catch (ConnectionCredentialsException e) {
      fail("Test failed: " + e.getMessage());
    }

    xmlResponse = new Element("root");

    myAwsTestConnectionController = new AwsTestConnectionController(
      Mockito.mock(SBuildServer.class),
      Mockito.mock(WebControllerManager.class),
      createTester(),
      Mockito.mock(ProjectManager.class)
    );
  }

  private AwsConnectionTester createTester() {
    return new AwsConnectionTester() {
      @NotNull
      @Override
      public AwsTestConnectionResult testConnection(@NotNull final ProjectFeatureDescriptorImpl connectionFeature) throws AmazonClientException {
        return new AwsTestConnectionResult(
          new GetCallerIdentityResult()
            .withAccount(testAccountId)
            .withArn(testArn)
            .withUserId(testUserId)
        );
      }

      @NotNull
      @Override
      public List<InvalidProperty> getInvalidProperties(@NotNull final Map<String, String> connectionProperties) {
        return myAwsConnectorFactory.getInvalidProperties(connectionProperties);
      }
    };
  }

  @Test
  public void givenAwsConnectionProperties_whenTesting_thenReturnCallerIdentity() {

    Map<String, String> defaultProperties = createDefaultProperties();
    Map<String, String[]> parametersMapMock = Mockito.mock(Map.class);
    when(parametersMapMock.keySet())
      .thenReturn(defaultProperties.keySet());

    defaultProperties.forEach((k, v) -> when(request.getParameter(k))
      .thenReturn(v));

    when(request.getParameterMap())
      .thenReturn(parametersMapMock);

    myAwsTestConnectionController.doPost(request, response, xmlResponse);

    List<Element> testResults = getAwsConnectionTestResultFromXmlResponse(xmlResponse);

    assertEquals(1, testResults.size());
    assertEquals(testAccountId, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID));
    assertEquals(testUserId, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_USER_ID));
    assertEquals(testArn, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_USER_ARN));
  }

  @Test
  public void givenAwsConnectionPropertiesWithCustomStsEndpoint_whenTesting_thenReturnCallerIdentity() {
    String whiteListedStsEndpoint1 = "https://someEndpoint1";
    String whiteListedStsEndpoint2 = "https://someEndpoint2";
    setInternalProperty(StsEndpointParamValidator.STS_ENDPOINTS_ALLOWLIST_PROPERTY_NAME, whiteListedStsEndpoint1 + "," + whiteListedStsEndpoint2);

    Map<String, String> defaultProperties = createDefaultProperties();
    defaultProperties.put(propertyPrefix + STS_ENDPOINT_PARAM, whiteListedStsEndpoint2);

    Map<String, String[]> parametersMapMock = Mockito.mock(Map.class);
    when(parametersMapMock.keySet())
      .thenReturn(defaultProperties.keySet());

    defaultProperties.forEach((k, v) -> when(request.getParameter(k))
      .thenReturn(v));

    when(request.getParameterMap())
      .thenReturn(parametersMapMock);

    myAwsTestConnectionController.doPost(request, response, xmlResponse);

    List<Element> testResults = getAwsConnectionTestResultFromXmlResponse(xmlResponse);

    assertEquals(1, testResults.size());
    assertEquals(testAccountId, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_ACCOUNT_ID));
    assertEquals(testUserId, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_USER_ID));
    assertEquals(testArn, testResults.get(0).getAttributeValue(AWS_CALLER_IDENTITY_ATTR_USER_ARN));
  }

  @Test
  public void givenAwsConnectionPropertiesWithCustomStsEndpoint_whenItsNotWhitelisted_thenReturnCorrespondingError() {
    Map<String, String> defaultProperties = createDefaultProperties();
    defaultProperties.put(propertyPrefix + STS_ENDPOINT_PARAM, "https://someEndpoint");
    defaultProperties.put(propertyPrefix + SESSION_CREDENTIALS_PARAM, "true");

    Map<String, String[]> parametersMapMock = Mockito.mock(Map.class);
    when(parametersMapMock.keySet())
      .thenReturn(defaultProperties.keySet());

    defaultProperties.forEach((k, v) -> when(request.getParameter(k))
      .thenReturn(v));

    when(request.getParameterMap())
      .thenReturn(parametersMapMock);

    myAwsTestConnectionController.doPost(request, response, xmlResponse);

    assertEquals("The STS endpoint is not a valid URL, please, provide a valid URL", xmlResponse.getValue());
  }

  @Test
  public void givenAwsConnectionProperties_withoutCredentialsType_thenReturnCorrespondingError() {
    myAwsTestConnectionController.doPost(request, response, xmlResponse);
    assertEquals("The credentials type null is not supported.", xmlResponse.getValue());
  }

  private List<Element> getAwsConnectionTestResultFromXmlResponse(Element xmlContent) {
    List<Element> xmlCollection = xmlContent.getChildren(AWS_CALLER_IDENTITY_ELEMENT);

    return new ArrayList<>(xmlCollection);
  }

  private Map<String, String> createDefaultProperties() {
    Map<String, String> res = new HashMap<>();
    res.put(propertyPrefix + ACCESS_KEY_ID_PARAM, testAccessKeyId);
    res.put(propertyPrefix + AwsAccessKeysParams.SECURE_SECRET_ACCESS_KEY_PARAM, testSecretAccessKey);
    res.put(propertyPrefix + AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, AwsCloudConnectorConstants.STATIC_CREDENTIALS_TYPE);
    res.put(propertyPrefix + AwsCloudConnectorConstants.REGION_NAME_PARAM, AwsCloudConnectorConstants.REGION_NAME_DEFAULT);
    res.put(propertyPrefix + AwsSessionCredentialsParams.SESSION_DURATION_PARAM, AwsSessionCredentialsParams.SESSION_DURATION_DEFAULT);
    res.put(propertyPrefix + STS_ENDPOINT_PARAM, STS_GLOBAL_ENDPOINT);
    return res;
  }
}
