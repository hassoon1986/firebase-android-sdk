// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.gradle.plugins.ci

import static com.google.firebase.gradle.plugins.measurement.MetricsServiceApi.Metric
import static com.google.firebase.gradle.plugins.measurement.MetricsServiceApi.Report

import com.google.firebase.gradle.plugins.measurement.coverage.XmlReportParser
import com.google.firebase.gradle.plugins.measurement.MetricsReportUploader
import com.google.firebase.gradle.plugins.measurement.TestLogFinder
import com.google.firebase.gradle.plugins.SdkUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

class CheckCoveragePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.configure(project.subprojects) {
            apply plugin: 'jacoco'

            jacoco {
                toolVersion = '0.8.5'
            }

            tasks.withType(Test) {
                jacoco {
                    excludes = ['jdk.internal.*']
                }
            }

            task('checkCoverage', type: JacocoReport) {
                dependsOn 'check'
                description 'Generates check coverage report and uploads to Codecov.io.'
                group 'verification'

                def excludes = [
                        '**/R.class',
                        '**/R$*.class',
                        '**/BuildConfig.*',
                        '**/proto/**',
                        '**Manifest*.*'
                ]
                classDirectories = files([
                        fileTree(dir: "$buildDir/intermediates/javac/release", excludes: excludes),
                        fileTree(dir: "$buildDir/tmp/kotlin-classes/release", excludes: excludes),
                ])
                sourceDirectories = files(['src/main/java', 'src/main/kotlin'])
                executionData = fileTree(dir: "$buildDir", includes: ['jacoco/*.exec'])
                reports {
                    html.enabled true
                    xml.enabled true
                }

                outputs.upToDateWhen { false }

                doFirst {
                    logger.quiet("Reports directory: ${it.project.jacoco.reportsDir}")
                }

                doLast {
                    upload it
                }
            }

            tasks.withType(Test) {
                jacoco.includeNoLocationClasses true
            }

        }
    }

    private def upload(task) {
        def sdk = SdkUtil.getFullName(task.project)
        def xmlReport = task.reports.xml.destination

        def results = new XmlReportParser(sdk, xmlReport).parse()
        def log = TestLogFinder.generateCurrentLogLink()
        def report = new Report(Metric.Coverage, results, log)

        new File(task.project.buildDir, 'coverage.json').withWriter {
            it.write(report.toJson())
        }
        MetricsReportUploader.upload(task.project, "${task.project.buildDir}/coverage.json")
    }
}
