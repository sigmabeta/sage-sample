// okio's published JS bundle references Node's `os`, `path` and `fs` modules. Webpack 5 no longer
// auto-polyfills core modules and the browser has no Node runtime, so resolve them explicitly:
// `fs`/`path` are only touched inside NodeJsFileSystem methods this app never calls, and `os` is
// reached by okio's `Path.SYSTEM_TEMPORARY_DIRECTORY` static initializer, so it gets the real
// browser-safe stub.
config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};
config.resolve.fallback.path = false;
config.resolve.fallback.fs = false;
config.resolve.fallback.os = require.resolve('os-browserify/browser');
