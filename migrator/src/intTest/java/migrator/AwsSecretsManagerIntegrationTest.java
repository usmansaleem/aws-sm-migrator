/* Licensed under Apache-2.0 2023. */
package migrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import aws.sm.migrator.AwsSecretsManager;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException;

class AwsSecretsManagerIntegrationTest {
  final Random random = new Random();

  @Test
  void canGetAllSecretsForPrefix() {
    List<Map.Entry<String, String>> secretsList;
    try (AwsSecretsManager awsSecretsManager =
        new AwsSecretsManager(false, URI.create("http://localhost:4566"))) {
      secretsList = awsSecretsManager.getAllSecretsForPrefix("test-aws-integration/");
    }
    assertThat(secretsList).hasSize(15);
  }

  @Test
  void canTransformSecretsToTargetPrefix() {
    List<Map.Entry<String, String>> sourcePrefixSecrets;
    try (AwsSecretsManager awsSecretsManager =
        new AwsSecretsManager(false, URI.create("http://localhost:4566"))) {
      final String sourcePrefix = "test-aws-integration/";
      final String targetPrefix = "target-test-integration/";

      final List<Map.Entry<String, String>> secretsList =
          awsSecretsManager.getAllSecretsForPrefix(sourcePrefix);
      assertThat(secretsList).hasSize(15);

      awsSecretsManager.transformSecrets(
          targetPrefix, secretsList, 10, false, Collections.emptyList());

      final List<Map.Entry<String, String>> transformedSecretsList =
          awsSecretsManager.getAllSecretsForPrefix(targetPrefix);
      assertThat(transformedSecretsList).hasSize(2);

      final String keys0 = transformedSecretsList.get(0).getValue();
      assertThat(keys0.lines()).hasSize(10);
      final String keys1 = transformedSecretsList.get(1).getValue();
      assertThat(keys1.lines()).hasSize(5);

      // assert source prefix are not deleted
      sourcePrefixSecrets = awsSecretsManager.getAllSecretsForPrefix(sourcePrefix);
    }
    assertThat(sourcePrefixSecrets).hasSize(15);
  }

  @Test
  void secretsUnderSourcePrefixAreDeletedAfterTransformation() {
    try (AwsSecretsManager awsSecretsManager =
        new AwsSecretsManager(false, URI.create("http://localhost:4566"))) {

      // create random secrets under source prefix
      final String sourcePrefix = "oldprefix/";
      final String targetPrefix = "newprefix/";
      for (int i = 0; i < 15; i++) {
        awsSecretsManager.createSecret(
            sourcePrefix,
            String.format("%06x", random.nextInt(0x1_000_000)),
            Collections.emptyList());
      }

      // assert that they are created successfully
      final List<Map.Entry<String, String>> secretsList =
          awsSecretsManager.getAllSecretsForPrefix(sourcePrefix);
      assertThat(secretsList).hasSize(15);

      // transform to target prefix
      awsSecretsManager.transformSecrets(
          targetPrefix, secretsList, 10, true, Collections.emptyList());

      // assert that secrets has been transformed
      final List<Map.Entry<String, String>> transformedSecretsList =
          awsSecretsManager.getAllSecretsForPrefix(targetPrefix);
      assertThat(transformedSecretsList).hasSize(2);

      final String keys0 = transformedSecretsList.get(0).getValue();
      assertThat(keys0.lines()).hasSize(10);
      final String keys1 = transformedSecretsList.get(1).getValue();
      assertThat(keys1.lines()).hasSize(5);

      // assert that source prefix has been deleted
      assertThatExceptionOfType(InvalidRequestException.class)
          .isThrownBy(() -> awsSecretsManager.getAllSecretsForPrefix(sourcePrefix));
    }
  }
}
