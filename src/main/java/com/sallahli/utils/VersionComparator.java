package com.sallahli.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VersionComparator {

    
    public static boolean isInRange(String version, String minVersion, String maxVersion) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }

        version = version.trim();

        // If no range specified, accept all versions
        if (minVersion == null && maxVersion == null) {
            return true;
        }

        try {
            // Check minimum version
            if (minVersion != null && !minVersion.trim().isEmpty()) {
                if (compareVersions(version, minVersion.trim()) < 0) {
                    return false;
                }
            }

            // Check maximum version
            if (maxVersion != null && !maxVersion.trim().isEmpty()) {
                if (compareVersions(version, maxVersion.trim()) > 0) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Error comparing versions: version={}, min={}, max={}", version, minVersion, maxVersion, e);
            return false;
        }
    }

    
    private static int compareVersions(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int part1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int part2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (part1 != part2) {
                return Integer.compare(part1, part2);
            }
        }

        return 0;
    }

    
    private static int parseVersionPart(String part) {
        try {
            // Remove any non-numeric prefix/suffix (e.g., "1-beta" -> "1")
            String numericPart = part.replaceAll("\\D.*$", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            log.debug("Could not parse version part: {}", part);
            return 0;
        }
    }
}

