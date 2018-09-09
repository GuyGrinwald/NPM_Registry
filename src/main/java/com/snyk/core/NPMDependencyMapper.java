package com.snyk.core;

import com.snyk.models.NPMPackage;
import com.snyk.models.Package;
import com.snyk.models.Version;
import com.snyk.storage.DependencyTreeStorage;
import com.snyk.utils.JsonHandler;
import com.snyk.utils.RestClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NPMDependencyMapper implements DependencyMapper {
    private static final String NPM_URL = "https://registry.npmjs.org";

    private final DependencyTreeStorage dependencyTreeCache;
    private final RestClient restClient;
    private final JsonHandler<NPMPackage> jsonHandler;

    @Autowired
    public NPMDependencyMapper(DependencyTreeStorage dependencyTreeCache) {
        this.dependencyTreeCache = dependencyTreeCache;
        this.restClient = new RestClient();
        this.jsonHandler = new JsonHandler(NPMPackage.class);
    }

    /**
     * Builds the tree of dependencies for a given package and version using NPM. No validation is done to make sure
     * the given package is valid or even exists. If it not (or can't be found in NPM) an empty tree is returned.
     * See <a href="https://www.npmjs.com/">npm registry</a>
     */
    @Override
    public Package getDependencyTree(String packageName, String version) {
        log.info("Building dependency tree for {}:{}", packageName, version);

        Package dependencyTree = buildDependencyTreeAsync(packageName, version);
        dependencyTreeCache.put(packageName, dependencyTree.getVersion().getMinimalVersion(), dependencyTree);

        log.info("Completed dependency search for {}:{}", packageName, version);

        return dependencyTree;
    }

    private Package buildDependencyTreeAsync(String packageName, String versionExpression) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
        Version version = Version.valueOf(versionExpression);
        Package root = Package.builder().name(packageName).version(version).build();

        ForkJoinTask<Package> treeBuildingTask = forkJoinPool.submit(new DependencyTreeBuildingTask(root));
        return treeBuildingTask.join();
    }

    @Deprecated
    private Package buildDependencyTreeIterative(String packageName, String versionExpression) {
        Version version = Version.valueOf(versionExpression);
        Queue<Package> packageInspectionQueue = new LinkedList<>();

        log.debug("Checking dependency tree for {}:{}", packageName, versionExpression);
        Package root = Package.builder().name(packageName).version(version).build();
        packageInspectionQueue.offer(root);

        while (!packageInspectionQueue.isEmpty()) {
            Package current = packageInspectionQueue.poll();

            Package cachedPackage = dependencyTreeCache.get(current.getName(), current.getVersion().getMinimalVersion());
            if (cachedPackage != null) {
                current = cachedPackage;
                break;
            }

            Set<Package> dependencies = getDependenciesFromNPM(current);
            current.setDependencies(dependencies);
            dependencyTreeCache.put(current.getName(), current.getVersion().getMinimalVersion(), current);
            dependencies.forEach(packageInspectionQueue::offer);

        }

        return root;
    }

    private Set<Package> getDependenciesFromNPM(Package aPackage) {
        // query npm for the package's data
        NPMPackage npmPackage;
        try {
            npmPackage = queryNPM(aPackage.getName(), aPackage.getVersion());
            aPackage.getVersion().setMinimalVersion(npmPackage.getVersion());

            if (MapUtils.isEmpty(npmPackage.getDependencies())) {
                return new HashSet<>();
            }

            return npmPackage.getDependencies().entrySet().stream()
                    .map((Map.Entry<String, String> entry) -> {
                        String dependencyName = entry.getKey();
                        Version dependencyVersion = Version.valueOf(entry.getValue());
                        return Package.builder().name(dependencyName).version(dependencyVersion).build();
                    })
                    .collect(Collectors.toSet());

        }  catch (IOException e) {
            log.error("Could not query for dependencies for {}:{} [{}]",
                    aPackage,
                    aPackage.getVersion().getOriginalExpression(),
                    aPackage.getVersion().getSearchExpression(),
                    e);
            return new HashSet<>();
        }
    }

    @Deprecated
    private Package buildDependencyTreeRecursive(String packageName, String versionExpression) {
        Version version = Version.valueOf(versionExpression);

        log.debug("Checking dependency tree for {}:{}", packageName, versionExpression);

        // check cached packages
        Package tree = dependencyTreeCache.get(packageName, version.getSearchExpression());
        if (tree != null) {
            log.info("Cached version found of {}:{}", packageName, version.getSearchExpression());
            return tree;
        }

        // query npm for the package's data
        NPMPackage npmPackage;
        try {
            npmPackage = queryNPM(packageName, version);
            version.setMinimalVersion(npmPackage.getVersion());
        }  catch (IOException e) {
            log.error("Could not query for dependencies for {}:{} [{}]",
                packageName,
                version.getOriginalExpression(),
                version.getSearchExpression(),
                e);
            return Package.builder().name(packageName).version(version).build();
        }

        // check if the npm's version is cached
        tree = dependencyTreeCache.get(packageName, npmPackage.getVersion());
        if (tree != null) {
            log.info("NPM cached version found of {}:{}", packageName, npmPackage.getVersion());
            return tree;
        }

        // if there are no dependencies
        if (MapUtils.isEmpty(npmPackage.getDependencies())) {
            return Package.builder().name(packageName).version(version).build();
        }

        Set<Package> dependencies = npmPackage.getDependencies().entrySet().stream()
                .map(entry -> {
                    Package aPackage = buildDependencyTreeRecursive(entry.getKey(), entry.getValue());
                    dependencyTreeCache.put(aPackage.getName(), aPackage.getVersion().getMinimalVersion(), aPackage);
                    return aPackage;
                })
                .collect(Collectors.toSet());

        return Package.builder()
                .name(packageName)
                .version(version)
                .dependencies(dependencies)
                .build();
    }

    /**
     * Queries NPM for information regarding the given package and version
     */
    private NPMPackage queryNPM(String packageName, Version version) throws IOException {
        log.info("Querying NPM for {}:{}", packageName, version.getSearchExpression());

        // acquire dependencies for the given package from npm
        String packageUrl = String.format("%s/%s/%s", NPM_URL, packageName, version.getSearchExpression());
        return restClient.get(packageUrl, jsonHandler);
    }

    /**
     * A ForkJoin task the given a package retrieves all it's dependencies
     */
    private class DependencyTreeBuildingTask extends RecursiveTask<Package> {
        private final Package parent;

        public DependencyTreeBuildingTask(Package parent) {
            this.parent = parent;
        }

        @Override
        protected Package compute() {
            log.debug("Checking dependency tree for {}:{}", parent.getName(), parent.getVersion().getMinimalVersion());

            Package cachedPackage = dependencyTreeCache.get(parent.getName(), parent.getVersion().getMinimalVersion());
            if (cachedPackage != null) {
                log.debug("Found {}:{} in cache", parent.getName(), parent.getVersion().getMinimalVersion());
                return parent;
            }

            Set<Package> dependencies = getDependenciesFromNPM(parent);

            if (dependencies.size() > 0) {
                dependencies = ForkJoinTask.invokeAll(createSubtasks(dependencies))
                        .stream()
                        .map(ForkJoinTask::join)
                        .collect(Collectors.toSet());
            }

            parent.setDependencies(dependencies);
            dependencyTreeCache.put(parent.getName(), parent.getVersion().getMinimalVersion(), parent);

            return parent;
        }

        private Collection<DependencyTreeBuildingTask> createSubtasks(Set<Package> dependencies) {
            List<DependencyTreeBuildingTask> dividedTasks = new ArrayList<>();

            for (Package dependency : dependencies) {
                dividedTasks.add(new DependencyTreeBuildingTask(dependency));
            }

            return dividedTasks;
        }
    }
}
