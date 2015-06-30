/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.common.spi;

import org.wso2.carbon.device.mgt.common.Group;
import org.wso2.carbon.device.mgt.common.GroupManagementException;

import java.util.List;

/**
 * This represents the service provider interface that has to be implemented by any of new
 * device type plugin implementation intended to be managed through CDM.
 */
public interface GroupManager {

    /**
     * Method to add group.
     *
     * @param group Metadata corresponding to the group being added
     * @throws GroupManagementException If some unusual behaviour is observed while adding a group
     */
    void addGroup(Group group) throws GroupManagementException;

    /**
     * Method to modify the metadata corresponding to group.
     *
     * @param group Modified group related metadata
     * @throws GroupManagementException If some unusual behaviour is observed while modify the group
     */
    void modifyGroup(Group group) throws GroupManagementException;

    /**
     * Method to disenroll a particular device from CDM.
     *
     * @param groupId of the group being to delete
     * @throws GroupManagementException If some unusual behaviour is observed while deleting a group
     */
    void removeGroup(int groupId) throws GroupManagementException;

    /**
     * Method to retrieve metadata of all groups.
     *
     * @return List of metadata corresponding to all groups
     * @throws GroupManagementException If some unusual behaviour is observed while obtaining the group list
     */
    List<Group> getAllGroups() throws GroupManagementException;

    /**
     * Method to retrieve metadata of a group corresponding to a group id.
     *
     * @param groupId of the group
     * @return Metadata corresponding to a particular group
     * @throws GroupManagementException If some unusual behaviour is observed obtaining the group object
     */
    Group getGroup(int groupId) throws GroupManagementException;

}
