/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@CommandLine.Command(
    name = "transform",
    description = "Transform secrets from source-prefix/ to target-prefix/",
    subcommands = CommandLine.HelpCommand.class,
    mixinStandardHelpOptions = true)
public class TransformSecrets implements Callable<Integer> {
  @Spec CommandSpec spec; // injected by PicoCLI

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

  @CommandLine.Option(
      names = {"-t", "--target-prefix"},
      paramLabel = "target-prefix/",
      description = "Target Secret Name Prefix where to create the secrets.",
      required = true)
  private String targetSercretNamePrefix;

  @CommandLine.Option(
      names = {"--delete-source-secrets"},
      description = "Delete secrets under source prefix after migration.")
  private boolean deleteSourcePrefix = false;

  @CommandLine.Option(
      names = {"-r", "--replicate-regions"},
      paramLabel = "<region>",
      description =
          "Replicate secrets to regions. The secrets will be attempted to be added/removed from these regions.",
      split = ",")
  private Collection<String> replicaRegions;

  private Integer numberOfKeys;

  public TransformSecrets() {}

  @CommandLine.Option(
      names = {"-n", "--number-of-keys"},
      paramLabel = "<NUMBER>",
      description =
          "Number of keys to store in single secret. Maximum size is 200. Defaults to ${DEFAULT-VALUE}.",
      defaultValue = "200")
  public void setNumberOfKeys(int numberOfKeys) {
    if (numberOfKeys <= 0 || numberOfKeys > 200) {
      throw new CommandLine.ParameterException(
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
        new AwsSecretsManager(dryRun, awsEndpointUrl)) {
      final List<Map.Entry<String, String>> secretsList =
          awsSecretsManager.getAllSecretsForPrefix(sourceSecretNamePrefix);
      awsSecretsManager.transformSecrets(
          targetSercretNamePrefix, secretsList, numberOfKeys, deleteSourcePrefix, replicaRegions);
    } catch (final Exception e) {
      System.err.println("Error encountered: " + e.getMessage());
      e.printStackTrace();
      return -1;
    }
    return 0;
  }
}
