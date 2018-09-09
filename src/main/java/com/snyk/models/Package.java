package com.snyk.models;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

@Data
public class Package {
    private String name;
    private Version version;
    private Set<Package> dependencies;

    @Builder
    public Package(String name, Version version, Set<Package> dependencies) {
        this.name = name;
        this.version = version;
        this.dependencies = CollectionUtils.isEmpty(dependencies) ? new HashSet<>() : dependencies;
    }
}
