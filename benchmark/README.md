```
initscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.instamotor:mirakle:x.y.z'
        classpath 'com.instamotor:mirakle-benchmark:1.0.1'
    }
}

apply plugin: MirakleBenchmark // must be applied before Mirakle!
apply plugin: Mirakle

rootProject {
    def benchmarkResultsFolder = "build/mirakle-benchmark-results"

    mirakleBenchmark {
        name "home_to_c5.2xlarge"
        launchNumber 3
        resultsFolder = benchmarkResultsFolder
    }

    mirakle {
        host "aws_c5.2xlarge"
        excludeCommon += [benchmarkResultsFolder]
    }
}
```
`> ./gradlew clean build mirakleBenchmark`
```
----- LAUNCH № 1 -----
Init     : 3.54 secs
Upload   : 0.148 secs
Execute  : 38.332 secs
Download : 7.326 secs
Total    : 52.687 secs
-----------------------

----- LAUNCH № 2 -----
Init     : 0.92 secs
Upload   : 0.15 secs
Execute  : 38.39 secs
Download : 10.934 secs
Total    : 51.474 secs
-----------------------

----- LAUNCH № 3 -----
Init     : 0.322 secs
Upload   : 0.147 secs
Execute  : 39.162 secs
Download : 7.932 secs
Total    : 53.074 secs
-----------------------
```