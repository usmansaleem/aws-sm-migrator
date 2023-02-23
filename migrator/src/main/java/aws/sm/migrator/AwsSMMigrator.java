/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "AWSSMMigrator",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Migrates AWS Secret values to line-terminated multi-values.")
public class AwsSMMigrator implements Callable<Integer> {

  @Option(
      names = {"-s", "--source-prefix"},
      paramLabel = "source-prefix/",
      description = "Source Secret Name Prefix which contains the secrets.",
      required = true)
  private String sourceSecretNamePrefix;

  @Option(
      names = {"-t", "--target-prefix"},
      paramLabel = "target-prefix/",
      description = "Target Secret Name Prefix where to create the secrets.",
      required = true)
  private String targetSercretNamePrefix;

  @Option(
      names = {"-d", "--dry-run"},
      description = "Dry run only.")
  private boolean dryRun = false;

  @Option(
      names = {"-e", "--aws-endpoint-override-uri"},
      paramLabel = "<URI>",
      description = "Override AWS endpoint. Useful for integration testing with localstack.")
  private java.net.URI awsEndpointUrl;

  @Override
  public Integer call() {
    try (final AwsSecretsManager awsSecretsManager =
        new AwsSecretsManager(dryRun, awsEndpointUrl)) {
      final List<String> secretsList =
          awsSecretsManager.getAllSecretsForPrefix(sourceSecretNamePrefix);
      awsSecretsManager.transformSecrets(targetSercretNamePrefix, secretsList);
    } catch (Exception e) {
      System.err.println("Error encountered: " + e.getMessage());
      e.printStackTrace();
      return -1;
    }
    return 0;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new AwsSMMigrator()).execute(args);
    System.exit(exitCode);
  }
}
