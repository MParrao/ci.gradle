/**
 * (C) Copyright IBM Corporation 2014, 2015.
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

import com.ibm.wsspi.kernel.embeddable.Server
import com.ibm.wsspi.kernel.embeddable.ServerBuilder
import com.ibm.wsspi.kernel.embeddable.Server.Result
import com.ibm.wsspi.kernel.embeddable.ServerEventListener
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent
import com.ibm.wsspi.kernel.embeddable.ServerEventListener.ServerEvent.Type

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

import org.gradle.api.logging.LogLevel

class Liberty implements Plugin<Project> {

    void apply(Project project) {

        project.plugins.apply 'war'

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
                def params = buildLibertyMap(project);
                if (project.liberty.template != null && project.liberty.template.length() != 0) {
                    params.put('template', project.liberty.template)
                }
                executeServerCommand(project, 'create', params)
            }
        }

        project.task('libertyStart') {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                try {
                    params.put('clean', project.liberty.clean)
                    params.put('timeout', project.liberty.timeout)
                    executeServerCommand(project, 'start', params)
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
        project.tasks.clean.dependsOn project.tasks.libertyStop

        project.task('libertyPackage') {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                if (project.liberty.archive != null && project.liberty.archive.length() != 0) {
                    params.put('archive', new File(project.liberty.archive))
                }
                if (project.liberty.include != null && project.liberty.include.length() != 0) {
                    params.put('include',project.liberty.include)
                }
                executeServerCommand(project, 'package', params)
            }
        }

        project.task('libertyDump') {
            description 'Dump diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                if (project.liberty.archive != null && project.liberty.archive.length() != 0) {
                    params.put('archive', new File(project.liberty.archive))
                }
                if (project.liberty.include != null && project.liberty.include.length() != 0) {
                    params.put('include',project.liberty.include)
                }
                executeServerCommand(project, 'dump', params)
            }
        }

        project.task('libertyJavaDump') {
            description 'Dump diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                if (project.liberty.include != null && project.liberty.include.length() != 0) {
                    params.put('include',project.liberty.include)
                }
                executeServerCommand(project, 'javadump', params)
            }
        }

        project.task('libertyDebug') {
            description 'Run the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            doLast {
                executeServerCommand(project, 'debug', buildLibertyMap(project))
            }
        }

        project.task('deployWar') {
            description 'Deploys a WAR file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project);
                params.put('timeout', project.liberty.timeout)
                params.put('file', project.war.archivePath)
                project.ant.taskdef(name: 'deploy', 
                                    classname: 'net.wasdev.wlp.ant.DeployTask', 
                                    classpath: project.buildscript.configurations.classpath.asPath)
                project.ant.deploy(params)
            }
        }

        project.task('undeployWar') {
            description 'Removes a WAR file from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            doLast {
                def params = buildLibertyMap(project)
                params.put('timeout', project.liberty.timeout)
                params.put('file', project.war.archivePath.name)
                project.ant.taskdef(name: 'undeploy', 
                                    classname: 'net.wasdev.wlp.ant.UndeployTask', 
                                    classpath: project.buildscript.configurations.classpath.asPath)
                project.ant.undeploy(params)
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

    private void executeServerCommand(Project project, String command, Map<String, String> params) {
        project.ant.taskdef(name: 'server', 
                            classname: 'net.wasdev.wlp.ant.ServerTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        params.put('operation', command)
        project.ant.server(params)
    }

    private ServerBuilder getServerBuilder(Project project) {
        ServerBuilder sb = new ServerBuilder()
        sb.setName(project.liberty.serverName)
        sb.setUserDir(getUserDir(project))
        if (project.liberty.outputDir != null) {
            sb.setOutputDir(new File(project.liberty.outputDir))
        }
        return sb
    }


    private Map<String, String> buildLibertyMap(Project project) {

        Map<String, String> result = new HashMap();
        result.put('serverName', project.liberty.serverName)
        def libertyUserDirFile = getUserDir(project)
        if (!libertyUserDirFile.isDirectory()) {
            libertyUserDirFile.mkdirs()
        }
        result.put('userDir', libertyUserDirFile)
        result.put('installDir', project.liberty.wlpDir)
        if (project.liberty.outputDir != null) {
            result.put('outputDir', project.liberty.outputDir)
        }          

        return result;
    }
    
    private File getUserDir(Project project) {
        String wlpDir = project.liberty.wlpDir == null ? "" : project.liberty.wlpDir
        return (project.liberty.userDir == null) ? new File(wlpDir, 'usr') : new File(project.liberty.userDir)
    }

    private static class LibertyListener implements ServerEventListener {

        private BlockingQueue<ServerEvent> queue = new LinkedBlockingQueue<ServerEvent>()

        void serverEvent(ServerEvent event) {
            queue.put(event)
        }

        ServerEvent next() {
            return queue.take()
        }

    }


}
