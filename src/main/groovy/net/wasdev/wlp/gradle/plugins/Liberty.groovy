/**
 * (C) Copyright IBM Corporation 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.wasdev.wlp.gradle.plugins

import org.gradle.api.*


import com.ibm.wsspi.kernel.embeddable.ServerBuilder
import com.ibm.wsspi.kernel.embeddable.Server.Result
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent.Type
import org.gradle.api.logging.LogLevel
import net.wasdev.wlp.gradle.plugins.tasks.AbstractTask

class Liberty extends AbstractTask implements Plugin<Project> {

    void apply(Project project) {

        
        project.extensions.create('liberty', LibertyExtension)

        project.task('libertyRun') {
            description = "Runs a WebSphere Liberty Profile server under the Gradle process."
            doLast {
                ServerBuilder builder = getServerBuilder(project);

                LibertyListener listener = new LibertyListener()
                builder.setServerEventListener(listener)
                Result result = builder.build().start().get()
                if (!result.successful()) throw result.getException()

                while (!Type.STOPPED.equals(listener.next().getType())) {}
            }
        }

        project.task('libertyStatus') {
            description 'Checks the WebSphere Liberty Profile server is running.'
            logging.level = LogLevel.INFO
            doLast {
                try {
                    executeServerCommand(project, 'status', buildLibertyMap(project))
                } catch (Exception e) {
                    // Throws an exception if the server is stopped
                    println e
                }
            }
        }

        project.task('libertyCreate') {
            description 'Creates a WebSphere Liberty Profile server.'
            outputs.file { new File(getUserDir(project), "servers/${project.liberty.serverName}/server.xml") }
            logging.level = LogLevel.INFO
            doLast {
                executeServerCommand(project, 'create', buildLibertyMap(project))
            }
        }

        project.task('libertyStart') {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                try {
                    executeServerCommand(project, 'start', buildLibertyMap(project))
                } catch (Exception e) {
                    // Throws an exception if the server is already started
                    println e
                }
            }
        }

        project.task('libertyStop') {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                try {
                    executeServerCommand(project, 'stop', buildLibertyMap(project))
                } catch (Exception e) {
                    // Throws an exception if the server is already stopped
                    println e
                }
            }
        }
        
        project.task('libertyPackage') {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                params.put('archive', new File(project.buildDir, project.liberty.serverName + '.zip'))
                executeServerCommand(project, 'package', params)
            }
        }

        project.task('deploy') {
            description 'Deploys a WAR file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                project.ant.taskdef(name: 'deploy', 
                                    classname: 'net.wasdev.wlp.ant.DeployTask', 
                                    classpath: project.buildscript.configurations.classpath.asPath)
                                    
                if (project.liberty.file != null) {
                    params.put('file', project.liberty.file)
                    project.ant.deploy(params)
                } else {
                    String fsDir = project.liberty.fileSet.getDir()
                    if (fsDir != null) {
                        String includedPatterns = includedPatterns(project, project.liberty.fileSet) == null ? "" : includedPatterns(project, project.liberty.fileSet)
                        String excludedPatterns = excludedPatterns(project, project.liberty.fileSet) == null ? "" : excludedPatterns(project, project.liberty.fileSet)

                        project.ant.deploy(params) {
                            fileset(dir:fsDir, includes:includedPatterns, excludes:excludedPatterns)
                        }
                    } else {
                        project.ant.deploy(params)
                    }
                }
            }
        }

        project.task('undeploy') {
            description 'Removes a WAR file from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project)
                project.ant.taskdef(name: 'undeploy', 
                                    classname: 'net.wasdev.wlp.ant.UndeployTask', 
                                    classpath: project.buildscript.configurations.classpath.asPath)
                
                if (project.liberty.file != null) {
                    params.put('file', project.liberty.file)
                    project.ant.undeploy(params)
                } else {
                    String includedPatterns = includedPatterns(project, project.liberty.patternSet)
                    String excludedPatterns = excludedPatterns(project, project.liberty.patternSet)
                    
                    project.ant.undeploy(params) {
                        if (includedPatterns != null) {
                            patternset(includes:includedPatterns)
                        }
                        if (excludedPatterns != null) {
                            patternset(excludes:excludedPatterns)
                        }
                    }
                }
            }
        }
        
        project.task('installFeature') {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                params.put('name', project.liberty.featureName)
                params.put('acceptLicense', project.liberty.acceptLicense)
                if(project.liberty.whenFileExists != null) {
                    params.put('whenFileExists', project.liberty.whenFileExists)
                }
                params.put('to', project.liberty.to)
                params.remove('timeout')
                project.ant.taskdef(name: 'installFeature', 
                                   classname: 'net.wasdev.wlp.ant.InstallFeatureTask', 
                                   classpath: project.buildscript.configurations.classpath.asPath)
                project.ant.installFeature(params)
            }
        }
    }




}
