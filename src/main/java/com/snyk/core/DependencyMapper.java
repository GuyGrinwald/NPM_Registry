package com.snyk.core;

import com.snyk.models.Package;

public interface DependencyMapper {
    /**
     * Builds the tree of dependencies for a given package and version. No validation is done to make sure
     * the given package is valid or even exists. If it not (or can't be found) an empty tree is returned.
     */
    Package getDependencyTree(String packageName, String version);
}
