/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.Filter;
import software.amazon.awssdk.services.secretsmanager.model.FilterNameStringType;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import software.amazon.awssdk.services.secretsmanager.paginators.ListSecretsIterable;

public class AwsSecretsManager implements AutoCloseable {
  private static final SecureRandom random = new SecureRandom();
  private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
  private final SecretsManagerClient secretsClient;
  private final boolean dryRun;

  public AwsSecretsManager(final boolean dryRun, URI awsEndpointUrl) {
    this.secretsClient = getSecretsManagerClient(awsEndpointUrl);
    this.dryRun = dryRun;
  }

  public List<String> getAllSecretsForPrefix(String prefix) {
    System.out.printf("Fetching secrets under prefix %s%n", prefix);

    final List<Filter> filters = new ArrayList<>();
    filters.add(Filter.builder().key(FilterNameStringType.NAME).values(prefix).build());

    final ListSecretsIterable listSecretsResponses =
        secretsClient.listSecretsPaginator(ListSecretsRequest.builder().filters(filters).build());

    final List<String> secretValues = new ArrayList<>();

    listSecretsResponses.stream()
        .forEach(
            listSecretsResponse -> {
              final List<String> values =
                  listSecretsResponse.secretList().parallelStream()
                      .map(this::getSecretValue)
                      .toList();
              System.out.printf("Fetched %d secrets%n", values.size());
              secretValues.addAll(values);
            });

    return secretValues;
  }

  public void transformSecrets(final String targetPrefix, final List<String> secretValues) {
    List<List<String>> partitionedLists = Lists.partition(secretValues, 200);
    for (List<String> secretsList : partitionedLists) {
      String combinedSecrets =
          secretsList.stream().collect(Collectors.joining(System.lineSeparator()));

      // write combinedSecrets
      String secretName = randomString(targetPrefix);
      final CreateSecretRequest secretRequest =
          CreateSecretRequest.builder().name(secretName).secretString(combinedSecrets).build();

      System.out.printf("Writing %d secrets under %s ... ", secretsList.size(), secretName);
      if (!dryRun) {
        secretsClient.createSecret(secretRequest);
      }
      System.out.println("Created.");
    }
  }

  private String getSecretValue(final SecretListEntry secretListEntry) {
    GetSecretValueRequest valueRequest =
        GetSecretValueRequest.builder().secretId(secretListEntry.name()).build();

    GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
    return valueResponse.secretString();
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
  static String randomString(final String prefix) {
    final byte[] buffer = new byte[20];
    random.nextBytes(buffer);
    final String encoded = encoder.encodeToString(buffer);
    final String format = prefix.endsWith("/") ? "%s%s" : "%s/%s";
    return String.format(format, prefix, encoded);
  }
}
