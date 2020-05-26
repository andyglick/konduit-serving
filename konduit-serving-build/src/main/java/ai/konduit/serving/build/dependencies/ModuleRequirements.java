/*
 *  ******************************************************************************
 *  * Copyright (c) 2020 Konduit K.K.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package ai.konduit.serving.build.dependencies;

import ai.konduit.serving.build.config.Target;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
public class ModuleRequirements {

    private final List<DependencyRequirement> reqs;

    public boolean satisfiedBy(Target target, Collection<Dependency> currentDeps){
        for(DependencyRequirement req : reqs){
            if(!req.satisfiedBy(target, currentDeps))
                return false;
        }
        return true;
    }

    public Object suggestDependencies(Target target, Collection<Dependency> currentDeps){
        if(satisfiedBy(target, currentDeps))
            return null;


        throw new UnsupportedOperationException("Not yet implemented");
    }

}
