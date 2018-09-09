package com.snyk.storage;

import com.snyk.models.Package;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryDependencyTreeStorage implements DependencyTreeStorage {
    private final Map<String, Package> cache;

    public InMemoryDependencyTreeStorage() {
        cache = new ConcurrentHashMap<>();
    }

    @Override
    public Package get(String packageName, String version) {
        return cache.get(String.format("%s@%s", packageName, version));
    }

    @Override
    public void put(String packageName, String version, Package dependencyTree) {
        cache.compute(String.format("%s@%s", packageName, version), (k, v) -> dependencyTree);
    }
}
