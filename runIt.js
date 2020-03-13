const process = require("process");
const libraryVersionsJs = require("./build/js/packages/library-versions/kotlin/library-versions");

libraryVersionsJs.org.araqnid.libraryversions.js.findLatestVersions(process.argv[2]);
