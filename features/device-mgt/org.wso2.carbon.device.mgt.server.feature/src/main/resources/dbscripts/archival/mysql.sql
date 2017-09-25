
CREATE TABLE IF NOT EXISTS DM_OPERATION_ARCH (
    ID INTEGER NOT NULL,
    TYPE VARCHAR(20) NOT NULL,
    CREATED_TIMESTAMP TIMESTAMP NOT NULL,
    RECEIVED_TIMESTAMP TIMESTAMP NULL,
    OPERATION_CODE VARCHAR(50) NOT NULL,
    ARCHIVED_AT TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (ID)
)ENGINE = InnoDB;


CREATE TABLE IF NOT EXISTS DM_ENROLMENT_OP_MAPPING_ARCH (
    ID INTEGER NOT NULL,
    ENROLMENT_ID INTEGER NOT NULL,
    OPERATION_ID INTEGER NOT NULL,
    STATUS VARCHAR(50) NULL,
    PUSH_NOTIFICATION_STATUS VARCHAR(50) NULL,
    CREATED_TIMESTAMP INTEGER NOT NULL,
    UPDATED_TIMESTAMP INTEGER NOT NULL,
    ARCHIVED_AT TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (ID)
)ENGINE = InnoDB;


CREATE TABLE IF NOT EXISTS DM_DEVICE_OPERATION_RESPONSE_ARCH  (
   ID  INT(11) NOT NULL,
   ENROLMENT_ID  INTEGER NOT NULL,
   OPERATION_ID  INTEGER NOT NULL,
   EN_OP_MAP_ID  INTEGER NOT NULL,
   OPERATION_RESPONSE  LONGBLOB DEFAULT NULL,
   RECEIVED_TIMESTAMP  TIMESTAMP NULL,
   ARCHIVED_AT TIMESTAMP DEFAULT NOW(),
   PRIMARY KEY (ID)
)ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS DM_NOTIFICATION_ARCH (
    NOTIFICATION_ID INTEGER NOT NULL,
    DEVICE_ID INTEGER NOT NULL,
    OPERATION_ID INTEGER NOT NULL,
    TENANT_ID INTEGER NOT NULL,
    STATUS VARCHAR(10) NULL,
    DESCRIPTION VARCHAR(1000) NULL,
    ARCHIVED_AT TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (NOTIFICATION_ID)
)ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS DM_COMMAND_OPERATION_ARCH (
    OPERATION_ID INTEGER NOT NULL,
    ENABLED BOOLEAN NOT NULL DEFAULT FALSE,
    ARCHIVED_AT TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (OPERATION_ID)
)ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS DM_PROFILE_OPERATION_ARCH (
    OPERATION_ID INTEGER NOT NULL,
    ENABLED INTEGER NOT NULL DEFAULT 0,
    OPERATION_DETAILS BLOB DEFAULT NULL,
    ARCHIVED_AT TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (OPERATION_ID)
)ENGINE = InnoDB;
