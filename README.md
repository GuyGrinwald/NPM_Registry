# NPM_Registry

An implementation for the problem presented [here](https://github.com/snyk/jobs/blob/master/exercises/npm-registry.md).

## Some key notes:
1. The url for running the service is http://localhost:8080/registry/{package}/{version}
2. Due to time constraints I use HTTP, but in real world scenarios this would be HTTPS, with tokens and throttling etc.
3. I've implemented an in-memory storage for the purpose of the assignment but when scale is a consideration we would probably use other caching services (e.g. Redis, SQL DB etc.)
4. I've implemented 3 ways of doing the analysis - the first one was my earliest attempt and I improved upon it as I went on.
    1. Recursive dependencies parsing
    2. Iterative dependencies parsing
    3. Multi threaded divide and conqure (fork/join) dependencies parsing
5. Given more time I would add tests, more logs, and some way to make sure if 2 requests are for the same library, and arrive at the same time, only one would be computed while the other would wait for the result
6. I've used [Lombok](https://projectlombok.org/) so you need to have it installed or the code won't compile.
