/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.console.ng.ht.backend.server;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.console.ng.ht.model.TaskSummary;

public class TaskSummaryHelper {
    public static List<TaskSummary> adaptCollection(List<org.kie.api.task.model.TaskSummary> taskSums) {
        return adaptCollection(taskSums,false);
    }
    
    public static List<TaskSummary> adaptCollection(List<org.kie.api.task.model.TaskSummary> taskSums,boolean isForAdmin) {
        List<TaskSummary> taskSummaries = new ArrayList<TaskSummary>(taskSums.size());
        for (org.kie.api.task.model.TaskSummary taskSum : taskSums) {
            taskSummaries.add(adapt(taskSum,isForAdmin));
        }
        return taskSummaries;
    }
    
    public static TaskSummary adapt(org.kie.api.task.model.TaskSummary taskSum,boolean isForAdmin) {
        return new TaskSummary(
                    taskSum.getId(), 
                    taskSum.getName(),
                    taskSum.getDescription(),
                    taskSum.getStatusId(),
                    taskSum.getPriority(),
                    taskSum.getActualOwnerId(),
                    taskSum.getCreatedById(),
                    taskSum.getCreatedOn(),
                    taskSum.getActivationTime(),
                    taskSum.getExpirationTime(),
                    taskSum.getProcessId(),
                    taskSum.getProcessSessionId(),
                    taskSum.getProcessInstanceId(),
                    taskSum.getDeploymentId(), 
                    taskSum.getParentId(),
                    isForAdmin);
    
    }
}
