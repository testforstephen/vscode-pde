'use strict';

const path = require('path');
const fs = require('fs');
const gulp = require('gulp');
// const download = require('download');
const cp = require('child_process');
const server_dir = './pde';
const repo = server_dir + '/org.eclipse.jdt.ls.importer.pde.site/target/repository/plugins/'
const pluginGlobs = [
    repo + 'org.eclipse.equinox.p2.repository.tools_**',
    repo + 'org.eclipse.equinox.p2.publisher_**',
    repo + 'org.eclipse.ecf_**',
    repo + 'org.eclipse.equinox.p2.core_**',
    repo + 'org.eclipse.equinox.p2.touchpoint.natives_**',
    repo + 'org.sat4j.core_**',
    repo + 'org.eclipse.equinox.p2.transport.ecf_**',
    repo + 'org.eclipse.ecf.provider.filetransfer_**',
    repo + 'org.eclipse.ecf.identity_**',
    repo + 'org.sat4j.pb_**',
    repo + 'org.tukaani.xz_**',
    repo + 'org.eclipse.equinox.p2.garbagecollector_**',
    repo + 'org.eclipse.pde.core_**',
    repo + 'org.eclipse.pde.launching_**',
    repo + 'org.eclipse.jdt.ls.importer.pde_**',
    repo + 'org.eclipse.team.core_**',
    repo + 'org.eclipse.ecf.filetransfer_**',
    repo + 'org.eclipse.equinox.p2.touchpoint.eclipse_**',
    repo + 'org.eclipse.equinox.p2.jarprocessor_**',
    repo + 'org.eclipse.equinox.p2.metadata_**',
    repo + 'org.eclipse.equinox.p2.artifact.repository_**',
    repo + 'org.eclipse.pde.build_**',
    repo + 'org.eclipse.equinox.p2.director_**',
    repo + 'org.eclipse.equinox.p2.engine_**',
    repo + 'org.eclipse.equinox.concurrent_**',
    repo + 'org.eclipse.equinox.p2.metadata.repository_**',
    repo + 'org.eclipse.equinox.p2.publisher.eclipse_**',
    repo + 'org.eclipse.equinox.p2.repository_**',
    repo + 'bcpg_**',
    repo + 'bcprov_**',
    repo + 'bcutil_**',
    repo + 'org.eclipse.e4.core.contexts_**',
    repo + 'org.eclipse.e4.core.services_**',
    repo + 'org.eclipse.e4.core.di_**',
    repo + 'org.eclipse.e4.core.di.annotations_**',
    repo + 'org.eclipse.update.configurator_**',
    repo + 'org.eclipse.ecf.provider.filetransfer.httpclient5_**',
    repo + 'org.apache.httpcomponents.client5.httpclient5_**',
    repo + 'org.apache.httpcomponents.core5.httpcore5_**',
    repo + 'org.apache.httpcomponents.core5.httpcore5-h2_**',
    repo + 'org.eclipse.pde.junit.runtime_**',
    repo + 'org.eclipse.jdt.junit4.runtime_**',
    repo + 'javax.annotation_**',
    repo + 'javax.inject_**',
    repo + 'biz.aQute.bnd.util_**',
    repo + 'biz.aQute.bndlib_**',
    // repo + 'org.bndtools.versioncontrol.ignores.manager_**',
    // repo + 'bndtools.api_**',
    repo + 'jakarta.annotation-api_**',
    repo + 'jakarta.inject.jakarta.inject-api_**',
    repo + 'org.eclipse.m2e.pde.target_**',
    repo + 'org.osgi.service.repository_**',
    repo + 'org.apache.commons.commons-io_**'
];

gulp.task('patch_version', (cb) => {
    const packageJsonData = require('./package.json');
    const javaExtensions = packageJsonData.contributes.javaExtensions;
    if (Array.isArray(javaExtensions)) {
        packageJsonData.contributes.javaExtensions  = javaExtensions.map((extensionString) => {
            
            const ind = extensionString.indexOf('_');
            const fileName = findNewPDEPlugin(extensionString.substring(extensionString.lastIndexOf('/') + 1, ind));
            
            if (ind >= 0) {
                return extensionString.substring(0, extensionString.lastIndexOf('/') + 1) + fileName;
            }
            return extensionString;
        });

        fs.writeFileSync('./package.json', JSON.stringify(packageJsonData, null, 2));
    }
    cb();
});

// const m2eConnectorUrl = 'https://repo1.maven.org/maven2/.m2e/connectors/m2eclipse-tycho/0.8.1/N/0.8.1.201704211436/plugins/org.sonatype.tycho.m2e_0.8.1.201704211436.jar';
// gulp.task('download_tycho_m2e', async () => {
//     console.log("downloading " + m2eConnectorUrl);
//     return download(m2eConnectorUrl, './server');
// });

gulp.task('build_server', () => {
    cp.execSync(mvnw() + ' clean package', { cwd: server_dir, stdio: [0, 1, 2] });
    return gulp.src(pluginGlobs)
        .pipe(gulp.dest('./server'));
});

const full_build = gulp.series('build_server', 'patch_version');
gulp.task('full_build', full_build);

function isWin() {
    return /^win/.test(process.platform);
}

function isMac() {
    return /^darwin/.test(process.platform);
}

function isLinux() {
    return /^linux/.test(process.platform);
}

function mvnw() {
    // return "mvn";
    return isWin() ? "mvnw.cmd":"./mvnw";
}

function findPDEFullfileName() {
    const destFolder = path.resolve('./server');
    const files = fs.readdirSync(destFolder);
    const f = files.find((file) => {
        return file.indexOf('org.eclipse.jdt.ls.importer.pde') >= 0;
    });
    return f;
}


function findNewPDEPlugin(fileName) {
    fileName = fileName + "_";
    const destFolder = path.resolve('./server');
    const files = fs.readdirSync(destFolder);
    const f = files.find((file) => {
        return file.indexOf(fileName) >= 0;
    });
    console.log(f);
    return f;
}
