/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "delete",
    subcommands = CommandLine.HelpCommand.class,
    description = "Delete secrets from source-prefix/",
    mixinStandardHelpOptions = true)
public class DeleteSecrets implements Callable<Integer> {
  @CommandLine.Option(
      names = {"-s", "--source-prefix"},
      paramLabel = "source-prefix/",
      description = "Source Secret Name Prefix which contains the secrets.",
      required = true)
  private String sourceSecretNamePrefix;

  @CommandLine.Option(
      names = {"-d", "--dry-run"},
      description = "Dry run only.")
  private boolean dryRun = false;

  @CommandLine.Option(
      names = {"-e", "--aws-endpoint-override-uri"},
      paramLabel = "<URI>",
      description = "Override AWS endpoint. Useful for integration testing with localstack.")
  private java.net.URI awsEndpointUrl;

  @Override
  public Integer call() {
    try (final AwsSecretsManager awsSecretsManager =
        new AwsSecretsManager(dryRun, awsEndpointUrl)) {
      final List<Map.Entry<String, String>> secretsList =
          awsSecretsManager.getAllSecretsForPrefix(sourceSecretNamePrefix);
      awsSecretsManager.deleteSecrets(secretsList);
    } catch (final Exception e) {
      System.err.println("Error encountered: " + e.getMessage());
      e.printStackTrace();
      return -1;
    }
    return 0;
  }
}
