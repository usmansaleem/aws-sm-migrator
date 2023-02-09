package aws.sm.migrator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AwsSecretsManagerTest {

    @Test
    void randomStringIsGenerated() {
        String prefix = "target-prefix";
        String randomString = AwsSecretsManager.randomString(prefix);
        assertTrue(randomString.startsWith(prefix));
        assertTrue(randomString.length() > prefix.length());
        System.out.println(randomString);
    }
}