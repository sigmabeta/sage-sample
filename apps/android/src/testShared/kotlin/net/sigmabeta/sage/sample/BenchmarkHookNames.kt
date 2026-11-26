package net.sigmabeta.sage.sample

/**
 * The benchmark launch hook's class, by name. Every build type's unit tests see this one constant:
 * the benchmark test asserts the hook really has this name, and the debug and release tests assert
 * nothing by this name is on their classpath. A rename therefore fails the benchmark test instead of
 * quietly turning the absence check into a check for a class that never existed.
 */
const val BENCHMARK_SIGN_IN_CLASS = "net.sigmabeta.sage.sample.BenchmarkSignIn"
