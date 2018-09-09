package com.snyk.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Version {
    // see https://github.com/sindresorhus/semver-regex/blob/master/index.js
    private static final Pattern SEMVER_CAPTURE = Pattern.compile("((?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|" +
            "(?:[1-9]\\d*)|\\*)(?:-[\\da-z-]+(?:\\.[\\da-z-]+)*)?(?:\\+[\\da-z-]+(?:\\.[\\da-z-]+)*)?)");

    private String minimalVersion;
    private String originalExpression;
    @JsonIgnore
    private String searchExpression;


    private Version(String originalExpression, String searchExpression) {
        this.originalExpression = originalExpression;
        this.searchExpression = searchExpression;
    }

    @JsonIgnore
    public static Version valueOf(String semverVersion) {
        if (StringUtils.isEmpty(semverVersion)) {
            throw new IllegalArgumentException("Version can't be empty");
        }

        Matcher rangeCharactersMatcher = SEMVER_CAPTURE.matcher(semverVersion);
        if (rangeCharactersMatcher.find()) {
            String searchExpression = rangeCharactersMatcher.group(1);
            return new Version(semverVersion, searchExpression);
        }

        return new Version(semverVersion, semverVersion);
    }
}
