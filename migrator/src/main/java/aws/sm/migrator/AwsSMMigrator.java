/* Licensed under Apache-2.0 2023. */
package aws.sm.migrator;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "aws-sm-migrator",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Migrates AWS Secret values to line-terminated multi-values.",
    subcommands = {TransformSecrets.class, DeleteSecrets.class, CommandLine.HelpCommand.class},
    synopsisSubcommandLabel = "COMMAND")
public class AwsSMMigrator {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new AwsSMMigrator()).execute(args);
    System.exit(exitCode);
  }
}
