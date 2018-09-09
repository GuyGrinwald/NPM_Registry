package com.snyk.storage;

import com.snyk.models.Package;

public interface DependencyTreeStorage {
    Package get(String packageName, String version);

    void put(String packageName, String version, Package dependencyTree);
}
