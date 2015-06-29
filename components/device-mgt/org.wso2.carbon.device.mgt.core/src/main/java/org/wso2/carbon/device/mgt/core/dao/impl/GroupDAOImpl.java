/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.core.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.core.dao.GroupDAO;
import org.wso2.carbon.device.mgt.core.dao.GroupManagementDAOException;
import org.wso2.carbon.device.mgt.core.dto.Group;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class GroupDAOImpl implements GroupDAO {


    private static final Log log = LogFactory.getLog(GroupDAOImpl.class);
    private DataSource dataSource;

    public GroupDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addGroup(Group group) throws GroupManagementDAOException {

    }

    @Override
    public void updateGroup(Group group) throws GroupManagementDAOException {

    }

    @Override
    public void deleteGroup(int groupId) throws GroupManagementDAOException {

    }

    @Override
    public Group getGroup(int groupId) throws GroupManagementDAOException {
        return null;
    }

    @Override
    public List<Group> getGroups() throws GroupManagementDAOException {
        return null;
    }

    @Override
    public List<Group> getGroupListOfUser(String username, int tenantId) throws GroupManagementDAOException {
        return null;
    }

    @Override
    public int getGroupCount() throws GroupManagementDAOException {
        return 0;
    }

    @Override
    public List<Group> getGroupsByName(String groupName, int tenantId) throws GroupManagementDAOException {
        return null;
    }

    private Connection getConnection() throws GroupManagementDAOException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new GroupManagementDAOException(
                    "Error occurred while obtaining a connection from the group " +
                            "management metadata repository datasource", e);
        }
    }
}
