/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.net.URI;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.Filter;
import software.amazon.awssdk.services.secretsmanager.model.FilterNameStringType;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.RemoveRegionsFromReplicationRequest;
import software.amazon.awssdk.services.secretsmanager.model.ReplicaRegionType;
import software.amazon.awssdk.services.secretsmanager.model.ReplicateSecretToRegionsRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretsIterable;

public class AwsSecretsManager implements AutoCloseable {
  private static final SecureRandom random = new SecureRandom();
  private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
  private final SecretsManagerClient secretsClient;
  private final boolean dryRun;

  public AwsSecretsManager(final boolean dryRun, final URI awsEndpointUrl) {
    this.dryRun = dryRun;
    this.secretsClient = getSecretsManagerClient(awsEndpointUrl);
  }

  public List<Map.Entry<String, String>> getAllSecretsForPrefix(String prefix) {
    System.out.printf("Fetching secrets under prefix %s%n", prefix);

    final List<Filter> filters = new ArrayList<>();
    filters.add(Filter.builder().key(FilterNameStringType.NAME).values(prefix).build());

    final ListSecretsIterable listSecretsResponses =
        secretsClient.listSecretsPaginator(
            ListSecretsRequest.builder().includePlannedDeletion(false).filters(filters).build());

    final List<Map.Entry<String, String>> secretValues = new ArrayList<>();

    listSecretsResponses.stream()
        .forEach(
            listSecretsResponse -> {
              final List<Map.Entry<String, String>> values =
                  listSecretsResponse.secretList().parallelStream()
                      .map(this::getSecretValue)
                      .toList();
              System.out.printf("Fetched %d secrets%n", values.size());
              secretValues.addAll(values);
            });
    System.out.println("Total secrets fetched: " + secretValues.size());
    return secretValues;
  }

  public void transformSecrets(
      final String targetPrefix,
      final List<Map.Entry<String, String>> secretValues,
      final int numberOfKeysPerSecret,
      final boolean deleteSourcePrefix,
      final Collection<String> replicaRegions) {
    System.out.println("Transforming secrets: " + secretValues.size());
    int count = 0;
    List<List<Map.Entry<String, String>>> partitionedLists =
        Lists.partition(secretValues, numberOfKeysPerSecret);
    for (List<Map.Entry<String, String>> secretsList : partitionedLists) {
      String combinedSecrets =
          secretsList.stream()
              .map(Map.Entry::getValue)
              .collect(Collectors.joining(System.lineSeparator()));

      // write combinedSecrets
      count++;
      createSecret(targetPrefix, combinedSecrets, replicaRegions);
    }

    System.out.println("New Secrets created: " + count);
    if (deleteSourcePrefix) {
      deleteSecrets(secretValues, replicaRegions);
    }
  }

  @VisibleForTesting
  public void createSecret(
      final String targetPrefix, final String secretValue, final Collection<String> regions) {
    final String secretName = randomString(targetPrefix);
    final CreateSecretRequest secretRequest =
        CreateSecretRequest.builder().name(secretName).secretString(secretValue).build();

    System.out.printf(
        "Writing %d secrets under %s ... %n", secretValue.lines().count(), secretName);
    if (!dryRun) {
      secretsClient.createSecret(secretRequest);

      // replicate to other regions
      if (regions != null && !regions.isEmpty()) {
        System.out.printf("Replicating %s to regions: %s%n", secretName, String.join(",", regions));
        final Collection<ReplicaRegionType> replicaRegionTypes =
            regions.stream()
                .map(region -> ReplicaRegionType.builder().region(region).build())
                .toList();
        final ReplicateSecretToRegionsRequest replicateRequest =
            ReplicateSecretToRegionsRequest.builder()
                .secretId(secretName)
                .addReplicaRegions(replicaRegionTypes)
                .build();
        secretsClient.replicateSecretToRegions(replicateRequest);
      }
    }
    System.out.println("Created.");
  }

  void deleteSecrets(
      final List<Map.Entry<String, String>> secretsList, final Collection<String> replicaRegions) {
    System.out.println("Deleting source records: " + secretsList.size());
    secretsList.stream()
        .map(Map.Entry::getKey)
        .forEach(secretName -> deleteSecret(secretName, replicaRegions));
    System.out.println("Source records deleted.");
  }

  private void deleteSecret(final String secretName, final Collection<String> replicaRegions) {
    if (dryRun) {
      return;
    }
    try {
      System.out.println("Deleting " + secretName);
      // remove regions from replication
      if (replicaRegions != null && !replicaRegions.isEmpty()) {
        System.out.printf("Deleting from replication regions %s %n", String.join(", ", replicaRegions));
        final RemoveRegionsFromReplicationRequest request =
            RemoveRegionsFromReplicationRequest.builder()
                .secretId(secretName)
                .removeReplicaRegions(replicaRegions)
                .build();
        secretsClient.removeRegionsFromReplication(request);
      }

      final DeleteSecretRequest secretRequest =
          DeleteSecretRequest.builder().secretId(secretName).recoveryWindowInDays(7L).build();
      secretsClient.deleteSecret(secretRequest);
    } catch (SecretsManagerException e) {
      System.err.println("ERROR: " + e.awsErrorDetails().errorMessage());
    }
  }

  private Map.Entry<String, String> getSecretValue(final SecretListEntry secretListEntry) {
    final String secretName = secretListEntry.name();
    GetSecretValueRequest valueRequest =
        GetSecretValueRequest.builder().secretId(secretName).build();

    GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
    return new AbstractMap.SimpleEntry<>(secretName, valueResponse.secretString());
  }

  private static SecretsManagerClient getSecretsManagerClient(final URI awsEndpointUrl) {
    SecretsManagerClientBuilder builder =
        SecretsManagerClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(DefaultAwsRegionProviderChain.builder().build().getRegion());
    if (awsEndpointUrl != null) {
      builder.endpointOverride(awsEndpointUrl);
    }
    return builder.build();
  }

  @Override
  public void close() {
    if (secretsClient != null) {
      secretsClient.close();
    }
  }

  /**
   * 20 byte (160 bit) random value that is URL safe Base64 encoded.
   *
   * @param prefix to attach with the String. Appends '/' with prefix if it is missing.
   * @return random string
   */
  @VisibleForTesting
  public static String randomString(final String prefix) {
    final byte[] buffer = new byte[20];
    random.nextBytes(buffer);
    final String encoded = encoder.encodeToString(buffer);
    final String format = prefix.endsWith("/") ? "%s%s" : "%s/%s";
    return String.format(format, prefix, encoded);
  }
}
