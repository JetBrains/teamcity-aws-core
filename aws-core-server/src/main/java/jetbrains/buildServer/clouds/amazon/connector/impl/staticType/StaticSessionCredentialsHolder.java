package jetbrains.buildServer.clouds.amazon.connector.impl.staticType;

import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsData;
import jetbrains.buildServer.clouds.amazon.connector.AwsCredentialsHolder;
import jetbrains.buildServer.clouds.amazon.connector.impl.CredentialsRefresher;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.ParamUtil;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Map;

public class StaticSessionCredentialsHolder extends CredentialsRefresher {

  private final GetSessionTokenRequest mySessionConfiguration;

  private volatile GetSessionTokenResult currentSession;

  public StaticSessionCredentialsHolder(@NotNull final AwsCredentialsHolder credentialsHolder,
                                        @NotNull final Map<String, String> connectionProperties,
                                        @NotNull final ExecutorServices executorServices) {
    super(credentialsHolder, connectionProperties, executorServices);

    int sessionDurationMinutes = ParamUtil.getSessionDurationMinutes(connectionProperties);
    mySessionConfiguration = new GetSessionTokenRequest()
      .withDurationSeconds(sessionDurationMinutes * 60);

    currentSession = mySts.getSessionToken(mySessionConfiguration);
  }

  @NotNull
  @Override
  public AwsCredentialsData getAwsCredentials() {
    Credentials credentials = currentSession.getCredentials();
    return getDataFromCredentials(credentials);
  }

  @Override
  public void refreshCredentials() {
    Loggers.CLOUD.debug("Refreshing AWS Credentials...");
    try {
      currentSession = mySts.getSessionToken(mySessionConfiguration);
    } catch (Exception e) {
      Loggers.CLOUD.debug("Failed to refresh AWS Credentials: " + e.getMessage());
    }
  }

  @Override
  @NotNull
  public Date getSessionExpirationDate() {
    return currentSession.getCredentials().getExpiration();
  }
}
