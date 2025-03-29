package com.adieser.conntest.models.utils;

import com.adieser.conntest.configurations.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Component responsible for validating ping log files and their associated properties.
 * This validator ensures security and integrity of file operations by checking file names,
 * paths, and file properties against defined constraints.
 */
@Component
public class PingLogFileValidator {
    private static final Logger logger = LoggerFactory.getLogger(PingLogFileValidator.class);
    private final AppProperties appProperties;

    /**
     * Constructs a new PingLogFileValidator with the specified application properties.
     *
     * @param appProperties The application properties containing configuration settings
     *                     such as maximum file size limits
     */
    public PingLogFileValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Validates the provided file name according to security and formatting rules.
     * The method ensures the file name is not null, not blank, and follows valid naming conventions.
     *
     * @param newFileName The file name to validate, can be any Object that can be converted to String
     * @return The validated file name as a String
     * @throws IllegalArgumentException if the file name is null, blank, or doesn't meet validation criteria
     */
    public String validateFileName(Object newFileName) throws IllegalArgumentException {
        return Optional.ofNullable(newFileName)
                .map(Object::toString)
                .filter(name -> !name.isBlank())
                .filter(this::isValidFileName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or null file name provided"));
    }

    /**
     * Resolves and validates a file path relative to a base directory.
     * This method prevents path traversal attacks by ensuring the resolved path
     * stays within the specified base directory.
     *
     * @param baseDirectory The base directory that serves as the root for file operations
     * @param fileName The name of the file to resolve
     * @return The resolved and validated Path object
     * @throws SecurityException if the resolved path is outside the base directory
     *                          or if a path traversal attack is detected
     */
    public Path resolveAndValidatePath(Path baseDirectory, String fileName) throws SecurityException{
        Path normalizedPath = Path.of(fileName).normalize();
        Path resolvedPath = baseDirectory.resolve(normalizedPath).normalize();

        if (!resolvedPath.startsWith(baseDirectory)) {
            logger.warn("Attempted path traversal attack detected with file: {}", fileName);
            throw new SecurityException("Access denied. File must be within the allowed directory");
        }

        return resolvedPath;
    }

    /**
     * Validates various properties of a file including its size and type.
     * This method checks if:
     * - The file exists
     * - The file size is within the configured maximum limit
     * - The path points to a regular file (not a directory or special file)
     *
     * @param filePath The path to the file to validate
     * @throws FileSystemException if there are issues accessing the file
     * @throws SecurityException if the file violates size limits or is not a regular file
     */
    public void validateFileProperties(Path filePath) throws FileSystemException, SecurityException {
        // If file exists, validate its properties
        if (Files.exists(filePath)) {
            try {
                if (Files.size(filePath) > appProperties.getFileMaxSizeKbytes() * 1024) {
                    throw new SecurityException("File size exceeds maximum allowed size");
                }

                if (!Files.isRegularFile(filePath)) {
                    throw new SecurityException("Path must point to a regular file");
                }

                if (!Files.isReadable(filePath) || !Files.isWritable(filePath)) {
                    throw new SecurityException("Insufficient file permissions");
                }
            } catch (IOException e) {
                throw new FileSystemException("Failed to validate file properties: " + e.getMessage());
            }
        }
    }

    private boolean isValidFileName(String fileName) {
        return fileName != null &&
                fileName.toLowerCase().endsWith(".log") &&
                fileName.length() <= 255 &&
                fileName.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*\\.log$");
    }
}

