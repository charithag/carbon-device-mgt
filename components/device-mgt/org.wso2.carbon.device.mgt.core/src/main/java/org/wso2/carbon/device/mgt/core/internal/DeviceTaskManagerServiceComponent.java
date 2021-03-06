/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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


package org.wso2.carbon.device.mgt.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.device.mgt.common.DeviceStatusTaskPluginConfig;
import org.wso2.carbon.device.mgt.common.OperationMonitoringTaskConfig;
import org.wso2.carbon.device.mgt.core.config.DeviceConfigurationManager;
import org.wso2.carbon.device.mgt.core.config.DeviceManagementConfig;
import org.wso2.carbon.device.mgt.core.device.details.mgt.DeviceInformationManager;
import org.wso2.carbon.device.mgt.core.device.details.mgt.impl.DeviceInformationManagerImpl;
import org.wso2.carbon.device.mgt.core.dto.DeviceType;
import org.wso2.carbon.device.mgt.core.search.mgt.SearchManagerService;
import org.wso2.carbon.device.mgt.core.search.mgt.impl.SearchManagerServiceImpl;
import org.wso2.carbon.device.mgt.core.status.task.DeviceStatusTaskException;
import org.wso2.carbon.device.mgt.core.status.task.DeviceStatusTaskManagerService;
import org.wso2.carbon.device.mgt.core.status.task.impl.DeviceStatusTaskManagerServiceImpl;
import org.wso2.carbon.device.mgt.core.task.DeviceMgtTaskException;
import org.wso2.carbon.device.mgt.core.task.DeviceTaskManagerService;
import org.wso2.carbon.device.mgt.core.task.impl.DeviceTaskManagerServiceImpl;
import org.wso2.carbon.ntask.core.service.TaskService;

import java.util.ArrayList;
import java.util.Map;

/**
 * @scr.component name="org.wso2.carbon.device.task.manager" immediate="true"
 * @scr.reference name="device.ntask.component"
 * interface="org.wso2.carbon.ntask.core.service.TaskService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setTaskService"
 * unbind="unsetTaskService"
 */

public class DeviceTaskManagerServiceComponent {

    private static Log log = LogFactory.getLog(DeviceTaskManagerServiceComponent.class);

    @SuppressWarnings("unused")
    protected void activate(ComponentContext componentContext) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Initializing device task manager bundle.");
            }
            getDeviceOperationMonitoringConfig(componentContext);
            //Start the DeviceStatusMonitoringTask for registered DeviceTypes
            DeviceManagementConfig deviceManagementConfig = DeviceConfigurationManager.getInstance().
                    getDeviceManagementConfig();
            if (deviceManagementConfig != null && deviceManagementConfig.getDeviceStatusTaskConfig().isEnabled()) {
                startDeviceStatusMonitoringTask();
            }

            componentContext.getBundleContext().registerService(DeviceInformationManager.class,
                    new DeviceInformationManagerImpl(), null);

            componentContext.getBundleContext().registerService(SearchManagerService.class,
                    new SearchManagerServiceImpl(), null);
        } catch (Throwable e) {
            log.error("Error occurred while initializing device task manager service.", e);
        }
    }

    private void getDeviceOperationMonitoringConfig(ComponentContext componentContext)
            throws DeviceMgtTaskException {
        DeviceTaskManagerService deviceTaskManagerService = new DeviceTaskManagerServiceImpl();
        DeviceManagementDataHolder.getInstance().setDeviceTaskManagerService(deviceTaskManagerService);
        componentContext.getBundleContext().registerService(DeviceTaskManagerService.class,
                deviceTaskManagerService, null);
        Map<String, OperationMonitoringTaskConfig> deviceConfigMap = DeviceMonitoringOperationDataHolder
                .getInstance().getOperationMonitoringConfigFromMap();
        for (String platformType : new ArrayList<>(deviceConfigMap.keySet())) {
            if (deviceConfigMap.get(platformType).isEnabled()){
                deviceTaskManagerService.startTask(platformType, deviceConfigMap.get(platformType));
            }
            deviceConfigMap.remove(platformType);
        }
    }

    private void startDeviceStatusMonitoringTask() {
        DeviceStatusTaskManagerService deviceStatusTaskManagerService = new DeviceStatusTaskManagerServiceImpl();
        Map<DeviceType, DeviceStatusTaskPluginConfig> deviceStatusTaskPluginConfigs = DeviceManagementDataHolder.
                getInstance().getDeviceStatusTaskPluginConfigs();
        for (DeviceType deviceType : new ArrayList<>(deviceStatusTaskPluginConfigs.keySet())) {
            try {
                deviceStatusTaskManagerService.startTask(deviceType, deviceStatusTaskPluginConfigs.get(deviceType));
            } catch (DeviceStatusTaskException e) {
                log.error("Exception occurred while starting the DeviceStatusMonitoring Task for deviceType '" +
                        deviceType + "'", e);
            }
        }
    }

    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext componentContext) {
        try {
//            DeviceTaskManagerService taskManagerService = new DeviceTaskManagerServiceImpl();
//            taskManagerService.stopTask();
        } catch (Throwable e) {
            log.error("Error occurred while destroying the device details retrieving task manager service.", e);
        }
    }

    protected void setTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the task service.");
        }
        DeviceManagementDataHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Removing the task service.");
        }
        DeviceManagementDataHolder.getInstance().setTaskService(null);
    }
}