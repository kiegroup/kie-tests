/*
 * Copyright 2012 JBoss Inc
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
package org.kie.jbpm.test.usergroup.task;

import java.util.List;

import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;

/**
 * This implementation mimics the JAAS based implementation of user group callback used
 * in containers such as JBoss AS.
 * <p/>
 * Unlike the non-JAAS implementations, this will only return the groups of the "current user", which can be set using the
 * <code>setCurrentUser(String userId)</code> method.
 */
public class TestJaasUserGroupCallbackImpl extends JBossUserGroupCallbackImpl {

    private static volatile String currentUserId = null;

    public static void setCurrentUser(String userId) {
        currentUserId = userId;
    }

    public TestJaasUserGroupCallbackImpl(String propertiesPath) {
        super(propertiesPath);
    }

    public boolean existsUser(String userId) {
        // allows everything as there is no way to ask JAAS/JACC for users in the domain
        return true;
    }

    public boolean existsGroup(String groupId) {
        // allows everything as there is no way to ask JAAS/JACC for groups in the domain
        return true;
    }

    public List<String> getGroupsForUser(String userId, List<String> groupIds, List<String> allExistingGroupIds) {
        List<String> groups = groupStore.get(currentUserId);
        
        StringBuilder builder = new StringBuilder("Groups (");
        if (groups.size() > 0) {
            builder.append(groups.get(0));
            for (int i = 1; i < groups.size(); ++i) {
                builder.append(", " + groups.get(i));
            }
        }
        builder.append(") retrieved for current user '" + currentUserId + "'");
        System.out.println( builder.toString());
        
        return groups;
    }

}
