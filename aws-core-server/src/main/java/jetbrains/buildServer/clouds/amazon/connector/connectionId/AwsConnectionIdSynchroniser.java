package jetbrains.buildServer.clouds.amazon.connector.connectionId;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.serverSide.CustomDataStorage;
import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.NotNull;

public class AwsConnectionIdSynchroniser implements Runnable {

  public final static String AWS_CONNECTIONS_INCREMENTAL_ID_STORAGE = "aws.connections.current.incremental.id.storage";
  public final static String AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM = "awsConnectionsCurrentId";
  private final static int FIRST_INCREMENTAL_ID = 0;
  private final static Logger LOG = Logger.getInstance(AwsConnectionIdSynchroniser.class.getName());

  private final SProject myProject;
  private final AtomicInteger currentIdentifier = new AtomicInteger(-1);

  public AwsConnectionIdSynchroniser(@NotNull final SProject project) {
    myProject = project;
  }

  @Override
  public void run() {
    try {
      if (!currentIdentifierInitialised()) {
        loadIdentifier();
      } else {
        syncIdentifier();
      }
    } catch (Exception e) {
      LOG.warnAndDebugDetails("Cannot sync the current AWS identifier", e);
    }
  }

  public boolean currentIdentifierInitialised() {
    return currentIdentifier.get() != -1;
  }

  public int incrementAndGetCurrentIdentifier() {
    return currentIdentifier.incrementAndGet();
  }

  public void setInitialIdentifier() {
    CustomDataStorage dataStorage = getCustomDataStorage();
    dataStorage.putValue(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM, String.valueOf(FIRST_INCREMENTAL_ID));
    dataStorage.flush();
    currentIdentifier.set(FIRST_INCREMENTAL_ID);
  }

  public void syncIdentifier() {
    CustomDataStorage dataStorage = getCustomDataStorage();
    dataStorage.updateValues(
      Collections.singletonMap(
        AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM,
        String.valueOf(currentIdentifier.get())
      ),
      new HashSet<>()
    );
    dataStorage.flush();
  }

  @NotNull
  private CustomDataStorage getCustomDataStorage() {
    return myProject.getCustomDataStorage(AWS_CONNECTIONS_INCREMENTAL_ID_STORAGE);
  }

  private void loadIdentifier() {
    Map<String, String> dataStorageValues = getCustomDataStorage().getValues();
    try {
      if (dataStorageValues == null) {
        throw new NumberFormatException("There is no values in the  AWS Connections IDs DataStorage in the project: " + myProject.getExternalId());
      }
      int currentIdentifierFromDataStorage = Integer.parseInt(dataStorageValues.get(AWS_CONNECTIONS_CURRENT_INCREMENTAL_ID_PARAM));
      currentIdentifier.set(currentIdentifierFromDataStorage);

    } catch (NumberFormatException e) {
      LOG.warnAndDebugDetails("Wrong number in the incremental ID parameter of the CustomDataStorage in the Root Project", e);
    }
  }
}
