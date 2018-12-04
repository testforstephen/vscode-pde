'use strict';

const path = require('path');
const fs = require('fs');
const gulp = require('gulp');
const gulp_tslint = require('gulp-tslint');
const cp = require('child_process');
const server_dir = './pde';
const repo = server_dir + '/org.eclipse.jdt.ls.importer.pde.site/target/repository/plugins/'
const pluginGlobs = [
    repo + 'org.eclipse.equinox.p2.repository.tools_**',
    repo + 'org.eclipse.equinox.p2.publisher_**',
    repo + 'org.eclipse.ecf_**',
    repo + 'org.eclipse.equinox.p2.core_**',
    repo + 'org.eclipse.equinox.p2.touchpoint.natives_**',
    repo + 'org.eclipse.equinox.p2.director.app_**',
    repo + 'org.sat4j.core_**',
    repo + 'org.eclipse.equinox.p2.transport.ecf_**',
    repo + 'org.eclipse.ecf.provider.filetransfer_**',
    repo + 'org.apache.felix.scr_**',
    repo + 'org.eclipse.ecf.identity_**',
    repo + 'org.sat4j.pb_**',
    repo + 'org.tukaani.xz_**',
    repo + 'org.eclipse.equinox.p2.garbagecollector_**',
    repo + 'org.eclipse.pde.core_**',
    repo + 'org.eclipse.jdt.ls.importer.pde_**',
    repo + 'org.eclipse.team.core_**',
    repo + 'org.eclipse.ecf.filetransfer_**',
    repo + 'org.eclipse.equinox.p2.touchpoint.eclipse_**',
    repo + 'org.eclipse.equinox.p2.jarprocessor_**',
    repo + 'org.eclipse.equinox.p2.metadata_**',
    repo + 'org.eclipse.equinox.p2.artifact.repository_**',
    repo + 'org.eclipse.pde.build_**',
    repo + 'org.eclipse.equinox.ds_**',
    repo + 'org.eclipse.equinox.p2.director_**',
    repo + 'org.eclipse.equinox.p2.engine_**',
    repo + 'org.eclipse.equinox.concurrent_**',
    repo + 'org.eclipse.equinox.p2.metadata.repository_**',
    repo + 'org.eclipse.equinox.p2.publisher.eclipse_**',
    repo + 'org.eclipse.equinox.p2.repository_**',
    repo + 'org.eclipse.update.configurator_**'
];

gulp.task('tslint', () => {
    return gulp.src(['**/*.ts', '!**/*.d.ts', '!node_modules/**'])
        .pipe(gulp_tslint())
        .pipe(gulp_tslint.report());
});

gulp.task('patch_version', (cb) => {
    const fileName = findPDEFullfileName();
    console.log(fileName);
    const packageJsonData = require('./package.json');
    const javaExtensions = packageJsonData.contributes.javaExtensions;
    if (Array.isArray(javaExtensions)) {
        packageJsonData.contributes.javaExtensions  = javaExtensions.map((extensionString) => {
            const ind = extensionString.indexOf('org.eclipse.jdt.ls.importer.pde');
            if (ind >= 0) {
                return extensionString.substring(0, ind) + fileName;
            }
            return extensionString;
        });

        fs.writeFileSync('./package.json', JSON.stringify(packageJsonData, null, 2));
    }
    cb();
});

gulp.task('build_server', () => {
    cp.execSync(mvnw() + ' clean package', { cwd: server_dir, stdio: [0, 1, 2] });
    return gulp.src(pluginGlobs)
        .pipe(gulp.dest('./server'))
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
    return "mvn";
    // return isWin()?"mvnw.cmd":"./mvnw";
}

function findPDEFullfileName() {
    const destFolder = path.resolve('./server');
    const files = fs.readdirSync(destFolder);
    const f = files.find((file) => {
        return file.indexOf('org.eclipse.jdt.ls.importer.pde') >= 0;
    });
    return f;
}
