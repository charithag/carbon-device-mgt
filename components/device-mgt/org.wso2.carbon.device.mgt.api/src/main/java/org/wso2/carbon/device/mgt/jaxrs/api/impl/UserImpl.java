/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.device.mgt.jaxrs.api.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.PaginationRequest;
import org.wso2.carbon.device.mgt.core.service.DeviceManagementProviderService;
import org.wso2.carbon.device.mgt.core.service.EmailMetaInfo;
import org.wso2.carbon.device.mgt.jaxrs.api.common.MDMAPIException;
import org.wso2.carbon.device.mgt.jaxrs.api.util.CredentialManagementResponseBuilder;
import org.wso2.carbon.device.mgt.jaxrs.api.util.DeviceMgtAPIUtils;
import org.wso2.carbon.device.mgt.jaxrs.api.util.ResponsePayload;
import org.wso2.carbon.device.mgt.jaxrs.beans.UserCredentialWrapper;
import org.wso2.carbon.device.mgt.jaxrs.beans.UserWrapper;
import org.wso2.carbon.device.mgt.jaxrs.util.Constants;
import org.wso2.carbon.device.mgt.jaxrs.util.SetReferenceTransformer;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * This class represents the JAX-RS services of User related functionality.
 */
@SuppressWarnings("NonJaxWsWebServices")
public class UserImpl implements org.wso2.carbon.device.mgt.jaxrs.api.User {

    private static final String ROLE_EVERYONE = "Internal/everyone";
    private static Log log = LogFactory.getLog(UserImpl.class);

    /**
     * Method to add user to emm-user-store.
     *
     * @param userWrapper Wrapper object representing input json payload
     * @return {Response} Status of the request wrapped inside Response object
     */
    @Override
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addUser(UserWrapper userWrapper) {
        ResponsePayload responsePayload = new ResponsePayload();
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            if (userStoreManager.isExistingUser(userWrapper.getUsername())) {
                // if user already exists
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + userWrapper.getUsername() +
                            " already exists. Therefore, request made to add user was refused.");
                }
                // returning response with bad request state
                responsePayload.setStatusCode(HttpStatus.SC_CONFLICT);
                responsePayload.
                        setMessageFromServer("User by username: " + userWrapper.getUsername() +
                                " already exists. Therefore, request made to add user was refused.");
                return Response.status(Response.Status.CONFLICT).entity(responsePayload).build();
            } else {
                String initialUserPassword = generateInitialUserPassword();
                Map<String, String> defaultUserClaims =
                        buildDefaultUserClaims(userWrapper.getFirstname(), userWrapper.getLastname(),
                                userWrapper.getEmailAddress());
                // calling addUser method of carbon user api
                userStoreManager.addUser(userWrapper.getUsername(), initialUserPassword,
                        userWrapper.getRoles(), defaultUserClaims, null);
                // invite newly added user to enroll device
                inviteNewlyAddedUserToEnrollDevice(userWrapper.getUsername(), initialUserPassword);
                // Outputting debug message upon successful addition of user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + userWrapper.getUsername() + " was successfully added.");
                }
                // returning response with success state
                responsePayload.setStatusCode(HttpStatus.SC_CREATED);
                responsePayload.setMessageFromServer("User by username: " + userWrapper.getUsername() +
                        " was successfully added.");
                return Response.status(Response.Status.CREATED).entity(responsePayload).build();
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Exception in trying to add user by username: " + userWrapper.getUsername();
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * Method to get user information from emm-user-store.
     *
     * @param username User-name of the user
     * @return {Response} Status of the request wrapped inside Response object.
     */
    @Override
    @GET
    @Path("view")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUser(@QueryParam("username") String username) {
        ResponsePayload responsePayload = new ResponsePayload();
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            if (userStoreManager.isExistingUser(username)) {
                UserWrapper user = new UserWrapper();
                user.setUsername(username);
                user.setEmailAddress(getClaimValue(username, Constants.USER_CLAIM_EMAIL_ADDRESS));
                user.setFirstname(getClaimValue(username, Constants.USER_CLAIM_FIRST_NAME));
                user.setLastname(getClaimValue(username, Constants.USER_CLAIM_LAST_NAME));
                // Outputting debug message upon successful retrieval of user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + username + " was found.");
                }
                responsePayload.setStatusCode(HttpStatus.SC_OK);
                responsePayload.setMessageFromServer("User information was retrieved successfully.");
                responsePayload.setResponseContent(user);
                return Response.status(Response.Status.OK).entity(responsePayload).build();
            } else {
                // Outputting debug message upon trying to remove non-existing user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + username + " does not exist.");
                }
                // returning response with bad request state
                responsePayload.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                responsePayload.setMessageFromServer(
                        "User by username: " + username + " does not exist.");
                return Response.status(Response.Status.BAD_REQUEST).entity(responsePayload).build();
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Exception in trying to retrieve user by username: " + username;
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * Update user in user store
     *
     * @param userWrapper Wrapper object representing input json payload
     * @return {Response} Status of the request wrapped inside Response object.
     */
    @Override
    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateUser(UserWrapper userWrapper, @QueryParam("username") String username) {
        ResponsePayload responsePayload = new ResponsePayload();
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            if (userStoreManager.isExistingUser(userWrapper.getUsername())) {
                Map<String, String> defaultUserClaims =
                        buildDefaultUserClaims(userWrapper.getFirstname(), userWrapper.getLastname(),
                                userWrapper.getEmailAddress());
                if (StringUtils.isNotEmpty(userWrapper.getPassword())) {
                    // Decoding Base64 encoded password
                    byte[] decodedBytes = Base64.decodeBase64(userWrapper.getPassword());
                    userStoreManager.updateCredentialByAdmin(userWrapper.getUsername(),
                                                             new String(decodedBytes, StandardCharsets.UTF_8));
                    log.debug("User credential of username: " + userWrapper.getUsername() + " has been changed");
                }
                List<String> currentRoles = getFilteredRoles(userStoreManager, userWrapper.getUsername());
                List<String> newRoles = Arrays.asList(userWrapper.getRoles());

                List<String> rolesToAdd = new ArrayList<>(newRoles);
                List<String> rolesToDelete = new ArrayList<>();

                for (String role : currentRoles) {
                    if (newRoles.contains(role)) {
                        rolesToAdd.remove(role);
                    } else {
                        rolesToDelete.add(role);
                    }
                }
                rolesToDelete.remove(ROLE_EVERYONE);
                userStoreManager.updateRoleListOfUser(userWrapper.getUsername(),
                                                      rolesToDelete.toArray(new String[rolesToDelete.size()]),
                                                      rolesToAdd.toArray(new String[rolesToAdd.size()]));
                userStoreManager.setUserClaimValues(userWrapper.getUsername(), defaultUserClaims, null);
                // Outputting debug message upon successful addition of user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + userWrapper.getUsername() + " was successfully updated.");
                }
                // returning response with success state
                responsePayload.setStatusCode(HttpStatus.SC_CREATED);
                responsePayload.setMessageFromServer("User by username: " + userWrapper.getUsername() +
                        " was successfully updated.");
                return Response.status(Response.Status.CREATED).entity(responsePayload).build();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + userWrapper.getUsername() +
                            " doesn't exists. Therefore, request made to update user was refused.");
                }
                // returning response with bad request state
                responsePayload.setStatusCode(HttpStatus.SC_CONFLICT);
                responsePayload.
                        setMessageFromServer("User by username: " + userWrapper.getUsername() +
                                " doesn't  exists. Therefore, request made to update user was refused.");
                return Response.status(Response.Status.CONFLICT).entity(responsePayload).build();
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Exception in trying to update user by username: " + userWrapper.getUsername();
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * Private method to be used by addUser() to
     * generate an initial user password for a user.
     * This will be the password used by a user for his initial login to the system.
     *
     * @return {string} Initial User Password
     */
    private String generateInitialUserPassword() {
        int passwordLength = 6;
        //defining the pool of characters to be used for initial password generation
        String lowerCaseCharset = "abcdefghijklmnopqrstuvwxyz";
        String upperCaseCharset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numericCharset = "0123456789";
        Random randomGenerator = new Random();
        String totalCharset = lowerCaseCharset + upperCaseCharset + numericCharset;
        int totalCharsetLength = totalCharset.length();
        StringBuilder initialUserPassword = new StringBuilder();
        for (int i = 0; i < passwordLength; i++) {
            initialUserPassword
                    .append(totalCharset.charAt(randomGenerator.nextInt(totalCharsetLength)));
        }
        if (log.isDebugEnabled()) {
            log.debug("Initial user password is created for new user: " + initialUserPassword);
        }
        return initialUserPassword.toString();
    }

    /**
     * Method to build default user claims.
     *
     * @param firstname    First name of the user
     * @param lastname     Last name of the user
     * @param emailAddress Email address of the user
     * @return {Object} Default user claims to be provided
     */
    private Map<String, String> buildDefaultUserClaims(String firstname, String lastname, String emailAddress) {
        Map<String, String> defaultUserClaims = new HashMap<>();
        defaultUserClaims.put(Constants.USER_CLAIM_FIRST_NAME, firstname);
        defaultUserClaims.put(Constants.USER_CLAIM_LAST_NAME, lastname);
        defaultUserClaims.put(Constants.USER_CLAIM_EMAIL_ADDRESS, emailAddress);
        if (log.isDebugEnabled()) {
            log.debug("Default claim map is created for new user: " + defaultUserClaims.toString());
        }
        return defaultUserClaims;
    }

    /**
     * Method to remove user from emm-user-store.
     *
     * @param username Username of the user
     * @return {Response} Status of the request wrapped inside Response object.
     */
    @Override
    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    public Response removeUser(@QueryParam("username") String username) {
        ResponsePayload responsePayload = new ResponsePayload();
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            if (userStoreManager.isExistingUser(username)) {
                // if user already exists, trying to remove user
                userStoreManager.deleteUser(username);
                // Outputting debug message upon successful removal of user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + username + " was successfully removed.");
                }
                // returning response with success state
                responsePayload.setStatusCode(HttpStatus.SC_OK);
                responsePayload.setMessageFromServer(
                        "User by username: " + username + " was successfully removed.");
                return Response.status(Response.Status.OK).entity(responsePayload).build();
            } else {
                // Outputting debug message upon trying to remove non-existing user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + username + " does not exist for removal.");
                }
                // returning response with bad request state
                responsePayload.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                responsePayload.setMessageFromServer(
                        "User by username: " + username + " does not exist for removal.");
                return Response.status(Response.Status.BAD_REQUEST).entity(responsePayload).build();
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Exception in trying to remove user by username: " + username;
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * get all the roles except for the internal/xxx and application/xxx
     *
     * @param userStoreManager User Store Manager associated with the currently logged in user
     * @param username         Username of the currently logged in user
     * @return the list of filtered roles
     */
    private List<String> getFilteredRoles(UserStoreManager userStoreManager, String username) {
        String[] roleListOfUser = new String[0];
        try {
            roleListOfUser = userStoreManager.getRoleListOfUser(username);
        } catch (UserStoreException e) {
            e.printStackTrace();
        }
        List<String> filteredRoles = new ArrayList<>();
        for (String role : roleListOfUser) {
            if (!(role.startsWith("Internal/") || role.startsWith("Authentication/"))) {
                filteredRoles.add(role);
            }
        }
        return filteredRoles;
    }

    /**
     * Get user's roles by username
     *
     * @param username Username of the user
     * @return {Response} Status of the request wrapped inside Response object.
     */
    @Override
    @GET
    @Path("roles")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getRolesOfUser(@QueryParam("username") String username) {
        ResponsePayload responsePayload = new ResponsePayload();
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            if (userStoreManager.isExistingUser(username)) {
                responsePayload.setResponseContent(Collections.singletonList(getFilteredRoles(userStoreManager, username)));
                // Outputting debug message upon successful removal of user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + username + " was successfully removed.");
                }
                // returning response with success state
                responsePayload.setStatusCode(HttpStatus.SC_OK);
                responsePayload.setMessageFromServer(
                        "User roles obtained for user " + username);
                return Response.status(Response.Status.OK).entity(responsePayload).build();
            } else {
                // Outputting debug message upon trying to remove non-existing user
                if (log.isDebugEnabled()) {
                    log.debug("User by username: " + username + " does not exist for role retrieval.");
                }
                // returning response with bad request state
                responsePayload.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                responsePayload.setMessageFromServer(
                        "User by username: " + username + " does not exist for role retrieval.");
                return Response.status(Response.Status.BAD_REQUEST).entity(responsePayload).build();
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Exception in trying to retrieve roles for user by username: " + username;
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * Get the list of all users with all user-related info.
     *
     * @return A list of users
     */
    @Override
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAllUsers() {
        if (log.isDebugEnabled()) {
            log.debug("Getting the list of users with all user-related information");
        }
        List<UserWrapper> userList;
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            String[] users = userStoreManager.listUsers("*", -1);
            userList = new ArrayList<>(users.length);
            UserWrapper user;
            for (String username : users) {
                user = new UserWrapper();
                user.setUsername(username);
                user.setEmailAddress(getClaimValue(username, Constants.USER_CLAIM_EMAIL_ADDRESS));
                user.setFirstname(getClaimValue(username, Constants.USER_CLAIM_FIRST_NAME));
                user.setLastname(getClaimValue(username, Constants.USER_CLAIM_LAST_NAME));
                userList.add(user);
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Error occurred while retrieving the list of users";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        ResponsePayload responsePayload = new ResponsePayload();
        responsePayload.setStatusCode(HttpStatus.SC_OK);
        int count;
        count = userList.size();
        responsePayload.setMessageFromServer("All users were successfully retrieved. " +
                "Obtained user count: " + count);
        responsePayload.setResponseContent(userList);
        return Response.status(Response.Status.OK).entity(responsePayload).build();
    }

    /**
     * Get the list of all users with all user-related info.
     *
     * @return A list of users
     */
    @Override
    @GET
    @Path("{filter}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMatchingUsers(@PathParam("filter") String filter) {
        if (log.isDebugEnabled()) {
            log.debug("Getting the list of users with all user-related information using the filter : " + filter);
        }
        List<UserWrapper> userList;
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            String[] users = userStoreManager.listUsers(filter + "*", -1);
            userList = new ArrayList<>(users.length);
            UserWrapper user;
            for (String username : users) {
                user = new UserWrapper();
                user.setUsername(username);
                user.setEmailAddress(getClaimValue(username, Constants.USER_CLAIM_EMAIL_ADDRESS));
                user.setFirstname(getClaimValue(username, Constants.USER_CLAIM_FIRST_NAME));
                user.setLastname(getClaimValue(username, Constants.USER_CLAIM_LAST_NAME));
                userList.add(user);
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Error occurred while retrieving the list of users using the filter : " + filter;
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        ResponsePayload responsePayload = new ResponsePayload();
        responsePayload.setStatusCode(HttpStatus.SC_OK);
        int count;
        count = userList.size();
        responsePayload.setMessageFromServer("All users were successfully retrieved. " +
                "Obtained user count: " + count);
        responsePayload.setResponseContent(userList);
        return Response.status(Response.Status.OK).entity(responsePayload).build();
    }

    /**
     * Get the list of user names in the system.
     *
     * @return A list of user names.
     */
    @Override
    @GET
    @Path("view-users")
    public Response getAllUsersByUsername(@QueryParam("username") String userName) {
        if (log.isDebugEnabled()) {
            log.debug("Getting the list of users by name");
        }
        List<UserWrapper> userList;
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            String[] users = userStoreManager.listUsers("*" + userName + "*", -1);
            userList = new ArrayList<>(users.length);
            UserWrapper user;
            for (String username : users) {
                user = new UserWrapper();
                user.setUsername(username);
                user.setEmailAddress(getClaimValue(username, Constants.USER_CLAIM_EMAIL_ADDRESS));
                user.setFirstname(getClaimValue(username, Constants.USER_CLAIM_FIRST_NAME));
                user.setLastname(getClaimValue(username, Constants.USER_CLAIM_LAST_NAME));
                userList.add(user);
            }
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Error occurred while retrieving the list of users";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        ResponsePayload responsePayload = new ResponsePayload();
        responsePayload.setStatusCode(HttpStatus.SC_OK);
        int count;
        count = userList.size();
        responsePayload.setMessageFromServer("All users by username were successfully retrieved. " +
                "Obtained user count: " + count);
        responsePayload.setResponseContent(userList);
        return Response.status(Response.Status.OK).entity(responsePayload).build();
    }

    /**
     * Get the list of user names in the system.
     *
     * @return A list of user names.
     */
    @Override
    @GET
    @Path("users-by-username")
    public Response getAllUserNamesByUsername(@QueryParam("username") String userName) {
        if (log.isDebugEnabled()) {
            log.debug("Getting the list of users by name");
        }
        List<String> userList;
        try {
            UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            String[] users = userStoreManager.listUsers("*" + userName + "*", -1);
            userList = new ArrayList<>(users.length);
            Collections.addAll(userList, users);
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Error occurred while retrieving the list of users";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        ResponsePayload responsePayload = new ResponsePayload();
        responsePayload.setStatusCode(HttpStatus.SC_OK);
        int count;
        count = userList.size();
        responsePayload.setMessageFromServer("All users by username were successfully retrieved. " +
                "Obtained user count: " + count);
        responsePayload.setResponseContent(userList);
        return Response.status(Response.Status.OK).entity(responsePayload).build();
    }

    /**
     * Gets a claim-value from user-store.
     *
     * @param username Username of the user
     * @param claimUri required ClaimUri
     * @return claim value
     */
    private String getClaimValue(String username, String claimUri) throws MDMAPIException {
        UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
        try {
            return userStoreManager.getUserClaimValue(username, claimUri, null);
        } catch (UserStoreException e) {
            throw new MDMAPIException("Error occurred while retrieving value assigned to the claim '" +
                    claimUri + "'", e);
        }
    }

    /**
     * Method used to send an invitation email to a new user to enroll a device.
     *
     * @param username Username of the user
     */
    private void inviteNewlyAddedUserToEnrollDevice(String username, String password) throws MDMAPIException {
        if (log.isDebugEnabled()) {
            log.debug("Sending invitation mail to user by username: " + username);
        }
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain)) {
            tenantDomain = "";
        }
        if (!username.contains("/")) {
            username = "/" + username;
        }
        String[] usernameBits = username.split("/");
        DeviceManagementProviderService deviceManagementProviderService = DeviceMgtAPIUtils.getDeviceManagementService();

        Properties props = new Properties();
        props.setProperty("username", usernameBits[1]);
        props.setProperty("domain-name", tenantDomain);
        props.setProperty("first-name", getClaimValue(username, Constants.USER_CLAIM_FIRST_NAME));
        props.setProperty("password", password);

        String recipient = getClaimValue(username, Constants.USER_CLAIM_EMAIL_ADDRESS);

        EmailMetaInfo metaInfo = new EmailMetaInfo(recipient, props);
        try {
            deviceManagementProviderService.sendRegistrationEmail(metaInfo);
        } catch (DeviceManagementException e) {
            String msg = "Error occurred while sending registration email to user '" + username + "'";
            log.error(msg, e);
            throw new MDMAPIException(msg, e);
        }
    }

    /**
     * Method used to send an invitation email to a existing user to enroll a device.
     *
     * @param usernames Username list of the users to be invited
     */
    @Override
    @POST
    @Path("email-invitation")
    @Produces({MediaType.APPLICATION_JSON})
    public Response inviteExistingUsersToEnrollDevice(List<String> usernames) {
        if (log.isDebugEnabled()) {
            log.debug("Sending enrollment invitation mail to existing user.");
        }
        DeviceManagementProviderService deviceManagementProviderService = DeviceMgtAPIUtils.getDeviceManagementService();
        try {
            for (String username : usernames) {
                String recipient = getClaimValue(username, Constants.USER_CLAIM_EMAIL_ADDRESS);

                Properties props = new Properties();
                props.setProperty("first-name", getClaimValue(username, Constants.USER_CLAIM_FIRST_NAME));
                props.setProperty("username", username);

                EmailMetaInfo metaInfo = new EmailMetaInfo(recipient, props);
                deviceManagementProviderService.sendEnrolmentInvitation(metaInfo);
            }
        } catch (DeviceManagementException | MDMAPIException e) {
            String msg = "Error occurred while inviting user to enrol their device";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        ResponsePayload responsePayload = new ResponsePayload();
        responsePayload.setStatusCode(HttpStatus.SC_OK);
        responsePayload.setMessageFromServer("Email invitation was successfully sent to user.");
        return Response.status(Response.Status.OK).entity(responsePayload).build();
    }

    /**
     * Get a list of devices based on the username.
     *
     * @param username Username of the device owner
     * @return A list of devices
     */
    @Override
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("devices")
    public Response getAllDeviceOfUser(@QueryParam("username") String username,
                                       @QueryParam("start") int startIdx, @QueryParam("length") int length) {
        DeviceManagementProviderService dmService;
        try {
            dmService = DeviceMgtAPIUtils.getDeviceManagementService();
            if (length > 0) {
                PaginationRequest request = new PaginationRequest(startIdx, length);
                request.setOwner(username);
                return Response.status(Response.Status.OK).entity(dmService.getDevicesOfUser(request)).build();
            }
            return Response.status(Response.Status.OK).entity(dmService.getDevicesOfUser(username)).build();
        } catch (DeviceManagementException e) {
            String msg = "Device management error";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * This method is used to retrieve the user count of the system.
     *
     * @return returns the count.
     * @
     */
    @Override
    @GET
    @Path("count")
    public Response getUserCount() {
        try {
            String[] users = DeviceMgtAPIUtils.getUserStoreManager().listUsers("*", -1);
            Integer count = 0;
            if (users != null) {
                count = users.length;
            }
            return Response.status(Response.Status.OK).entity(count).build();
        } catch (UserStoreException | MDMAPIException e) {
            String msg =
                    "Error occurred while retrieving the list of users that exist within the current tenant";
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
    }

    /**
     * API is used to update roles of a user
     *
     * @param username
     * @param userList
     * @return
     * @
     */
    @Override
    @PUT
    @Path("{roleName}/users")
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateRoles(@PathParam("roleName") String username, List<String> userList) {
        try {
            final UserStoreManager userStoreManager = DeviceMgtAPIUtils.getUserStoreManager();
            if (log.isDebugEnabled()) {
                log.debug("Updating the roles of a user");
            }
            SetReferenceTransformer<String> transformer = new SetReferenceTransformer<>();
            transformer.transform(Arrays.asList(userStoreManager.getRoleListOfUser(username)),
                    userList);
            final String[] rolesToAdd = transformer.getObjectsToAdd().toArray(new String[transformer.getObjectsToAdd().size()]);
            final String[] rolesToDelete = transformer.getObjectsToRemove().toArray(new String[transformer.getObjectsToRemove().size()]);

            userStoreManager.updateRoleListOfUser(username, rolesToDelete, rolesToAdd);
        } catch (UserStoreException | MDMAPIException e) {
            String msg = "Error occurred while saving the roles for user: " + username;
            log.error(msg, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Method to change the user password.
     *
     * @param credentials Wrapper object representing user credentials.
     * @return {Response} Status of the request wrapped inside Response object.
     * @
     */
    @Override
    @POST
    @Path("change-password")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response resetPassword(UserCredentialWrapper credentials) {
        return CredentialManagementResponseBuilder.buildChangePasswordResponse(credentials);
    }

    /**
     * Method to change the user password.
     *
     * @param credentials Wrapper object representing user credentials.
     * @return {Response} Status of the request wrapped inside Response object.
     * @
     */
    @Override
    @POST
    @Path("reset-password")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response resetPasswordByAdmin(UserCredentialWrapper credentials) {
        return CredentialManagementResponseBuilder.buildResetPasswordResponse(credentials);
    }

}