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
import org.apache.tools.ant.types.PatternSet
import org.apache.tools.ant.types.FileSet

class LibertyExtension {

    String wlpDir
    String outputDir
    String userDir
    String serverName = "defaultServer"
    
    String file
    PatternSet patternSet = new PatternSet()
    FileSet fileSet = new FileSet()
    
    String featureName
    boolean acceptLicense = false
    String whenFileExists
    String to = "usr"
}