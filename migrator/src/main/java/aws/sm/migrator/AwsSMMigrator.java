/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(
    name = "AWSSMMigrator",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Migrates AWS Secret values to line-terminated multi-values.")
public class AwsSMMigrator implements Callable<Integer> {
  @Spec CommandSpec spec; // injected by PicoCLI

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

  private Integer numberOfKeys;

  @Option(
      names = {"-d", "--dry-run"},
      description = "Dry run only.")
  private boolean dryRun = false;

  @Option(
      names = {"--delete-source-secrets"},
      description = "Delete secrets under source prefix after migration.")
  private boolean deleteSourcePrefix = false;

  @Option(
      names = {"-e", "--aws-endpoint-override-uri"},
      paramLabel = "<URI>",
      description = "Override AWS endpoint. Useful for integration testing with localstack.")
  private java.net.URI awsEndpointUrl;

  @Option(
      names = {"-n", "--number-of-keys"},
      paramLabel = "<NUMBER>",
      description =
          "Number of keys to store in single secret. Maximum size is 200. Defaults to ${DEFAULT-VALUE}.",
      defaultValue = "200")
  public void setNumberOfKeys(int numberOfKeys) {
    if (numberOfKeys <= 0 || numberOfKeys > 200) {
      throw new ParameterException(
          spec.commandLine(),
          String.format(
              "Invalid value '%s' for option '--number-of-keys': "
                  + "value must be between 1 to 200",
              numberOfKeys));
    }
    this.numberOfKeys = numberOfKeys;
  }

  @Override
  public Integer call() {
    try (final AwsSecretsManager awsSecretsManager =
        new AwsSecretsManager(dryRun, numberOfKeys, awsEndpointUrl, deleteSourcePrefix)) {
      final List<Map.Entry<String, String>> secretsList =
          awsSecretsManager.getAllSecretsForPrefix(sourceSecretNamePrefix);
      awsSecretsManager.transformSecrets(targetSercretNamePrefix, secretsList);
    } catch (final Exception e) {
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
