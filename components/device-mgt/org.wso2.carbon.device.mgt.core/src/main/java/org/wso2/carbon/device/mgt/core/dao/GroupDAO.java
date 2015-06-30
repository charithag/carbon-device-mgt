/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.core.dao;

import org.wso2.carbon.device.mgt.common.Group;

import java.util.List;

/**
 * This class represents the key operations associated with persisting group related information.
 */
public interface GroupDAO {

    /**
     * Add new group
     *
     * @param group new group
     * @throws GroupManagementDAOException
     */
    void addGroup(Group group) throws GroupManagementDAOException;

    /**
     * Update an existing group
     *
     * @param group updated group
     * @throws GroupManagementDAOException
     */
    void updateGroup(Group group) throws GroupManagementDAOException;

    /**
     * Delete an existing group
     *
     * @param groupId group Id to delete
     * @throws GroupManagementDAOException
     */
    void deleteGroup(int groupId) throws GroupManagementDAOException;

    /**
     * Get group by Id
     *
     * @param groupId id of required group
     * @return Group
     * @throws GroupManagementDAOException
     */
    Group getGroup(int groupId) throws GroupManagementDAOException;

    List<Group> getGroups() throws GroupManagementDAOException;

    /**
     * Get the list of Groups belongs to a user.
     *
     * @param username Requested user.
     * @return List of Groups of the user.
     * @throws GroupManagementDAOException
     */
    List<Group> getGroupListOfUser(String username, int tenantId) throws GroupManagementDAOException;

    /**
     * Get the count of Group
     *
     * @return Group count
     * @throws GroupManagementDAOException
     */
    int getGroupCount() throws GroupManagementDAOException;

    /**
     * Get the list of Groups that matches with the given Group name.
     *
     * @param groupName Name of the Group
     * @return List of Groups that matches with the given Group name.
     * @throws GroupManagementDAOException
     */
    List<Group> getGroupsByName(String groupName, int tenantId) throws GroupManagementDAOException;
}
