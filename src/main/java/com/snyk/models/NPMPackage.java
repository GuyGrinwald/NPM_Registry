package com.snyk.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NPMPackage {
    @JsonProperty("name")
    private String packageName;
    @JsonProperty("version")
    private String version;
    @JsonProperty("dependencies")
    private Map<String, String> dependencies;

    public List<Package> getDependenciesAsPackages() {
        return dependencies.entrySet().stream()
                .map(entry -> Package.builder().name(entry.getKey()).version(Version.valueOf(entry.getValue())).build())
                .collect(Collectors.toList());
    }
}
