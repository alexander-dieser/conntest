package com.adieser.conntest.models.utils;

import com.adieser.conntest.configurations.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingLogFileValidatorTest {

    private PingLogFileValidator validator;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setFileMaxSizeKbytes(1L); // 1 KB
        validator = new PingLogFileValidator(appProperties);
    }

    // ------------------------
    // validateFileName
    // ------------------------

    @ParameterizedTest
    @MethodSource("validFileNames")
    void validateFileName_validNames_returnSameName(String fileName) {
        assertEquals(fileName, validator.validateFileName(fileName));
    }

    static Stream<String> validFileNames() {
        return Stream.of(
                "test.log",
                "PingLog123.log",
                "file-01_ok.log",
                "A.log"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void validateFileName_invalidNames_throwException(Object fileName) {
        assertThrows(IllegalArgumentException.class, () -> validator.validateFileName(fileName));
    }

    static Stream<Object> invalidFileNames() {
        return Stream.of(
                null,
                "",
                " ",
                "bad.txt",
                ".log",
                "a/../file.log",
                "file#.log"
        );
    }

    // ------------------------
    // resolveAndValidatePath
    // ------------------------

    @Test
    void resolveAndValidatePath_validPaths_returnValid() {
        Path baseDir = Paths.get("target/test-logs").toAbsolutePath();
        String fileName = "test.log";
        Path resolved = validator.resolveAndValidatePath(baseDir, fileName);
        assertTrue(resolved.startsWith(baseDir));
        assertTrue(resolved.toString().endsWith(fileName));
    }

    @ParameterizedTest
    @MethodSource("invalidPaths")
    void resolveAndValidatePath_traversalAttempt_throwsSecurity(Path baseDir, String fileName) {
        assertThrows(SecurityException.class, () -> validator.resolveAndValidatePath(baseDir, fileName));
    }

    static Stream<Arguments> invalidPaths() {
        Path baseDir = Paths.get("target/test-logs").toAbsolutePath();
        return Stream.of(
                Arguments.of(baseDir, "../hack.log"),
                Arguments.of(baseDir, "../../escape.log"),
                Arguments.of(baseDir, "/etc/passwd")
        );
    }

    // ------------------------
    // validateFileProperties
    // ------------------------

    @Test
    void validateFileProperties_validFile_doesNotThrow() throws IOException {
        Path tempFile = Files.createTempFile("valid", ".log");
        Files.writeString(tempFile, "OK");
        validator.validateFileProperties(tempFile);
        Files.deleteIfExists(tempFile);
    }

    @Test
    void validateFileProperties_largeFile_throwsSecurityException() throws IOException {
        Path tempFile = Files.createTempFile("large", ".log");
        Files.write(tempFile, new byte[2000]);
        assertThrows(SecurityException.class, () -> validator.validateFileProperties(tempFile));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void validateFileProperties_nonRegularFile_throwsSecurityException() {
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"));
        assertThrows(SecurityException.class, () -> validator.validateFileProperties(dir));
    }
}
