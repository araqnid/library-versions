const process = require("process");
const libraryVersionsJs = require("./build/js/packages/library-versions/kotlin/library-versions");

libraryVersionsJs.org.araqnid.libraryversions.findLatestVersions(process.argv[2]);
