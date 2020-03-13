const process = require("process");
const libraryVersionsJs = require("../build/js/packages/library-versions-js/kotlin/library-versions-js");

libraryVersionsJs.org.araqnid.libraryversions.js.findLatestVersions(process.argv[2]);
