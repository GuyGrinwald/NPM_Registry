package com.snyk.web;

import com.snyk.core.DependencyMapper;
import com.snyk.models.Package;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping(path = "/registry")
public class NPMController {
    private final DependencyMapper dependencyMapper;

    @Autowired
    public NPMController(DependencyMapper dependencyMapper) {
        this.dependencyMapper = dependencyMapper;
    }

    @GetMapping(path = "/{package}/{version}")
    public Package buildDepedencyTree(@PathVariable("package") String packageName,
                                      @PathVariable("version") String version) {
        // validate package
        if (StringUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("Package name must not be empty");
        }

        // validate version
        if (StringUtils.isEmpty(version)) {
            throw new IllegalArgumentException("Version must not be empty");
        }

        return dependencyMapper.getDependencyTree(packageName, version);
    }
}
