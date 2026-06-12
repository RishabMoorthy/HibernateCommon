/* =============================================================
   Create schema if missing
   ============================================================= */

IF SCHEMA_ID('${schema}') IS NULL
BEGIN
    EXEC('CREATE SCHEMA ${schema}');
END;

/* =============================================================
   1. STUBSERVERQA_VSDETAILS
   Oracle source: STUBSERVERQA_VSDETAILS
   ============================================================= */

IF OBJECT_ID('${schema}.STUBSERVERQA_VSDETAILS', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.STUBSERVERQA_VSDETAILS (
        VSID                BIGINT NOT NULL,
        VSNAME              VARCHAR(255) NULL,
        PORT                INT NULL,
        LASTUPDATED         VARCHAR(30) NULL,
        USERNAME            VARCHAR(255) NULL,
        STATUS              VARCHAR(10) NULL,

        KEEPREQRESLOGS      VARCHAR(10) NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_KEEPREQRESLOGS DEFAULT 'No',

        KEEPREQRESLOGSDAYS  VARCHAR(20) NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_KEEPREQRESLOGSDAYS DEFAULT '15',

        SAVERESPTIME        VARCHAR(20) NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_SAVERESPTIME DEFAULT 'No',

        [GROUP]             VARCHAR(100) NULL,
        TAGS                VARCHAR(100) NULL,

        DELAYMODE           VARCHAR(50) NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_DELAYMODE DEFAULT 'FIXED',

        DELAY               BIGINT NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_DELAY DEFAULT 0,

        LOWERMS             BIGINT NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_LOWERMS DEFAULT 0,

        UPPERMS             BIGINT NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_UPPERMS DEFAULT 0,

        SIGMA               DECIMAL(10,4) NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_SIGMA DEFAULT 0.0,

        MEDIANMS            DECIMAL(10,4) NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_MEDIANMS DEFAULT 0.0,

        TOTALTXN            INT NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_TOTALTXN DEFAULT 0,

        DELAYPERCENT        INT NULL
            CONSTRAINT DF_STUBSERVERQA_VSDETAILS_DELAYPERCENT DEFAULT 0,

        DATASOURCEENABLED   VARCHAR(10) NULL,

        CONSTRAINT PK_STUBSERVERQA_VSDETAILS PRIMARY KEY (VSID)
    );
END;

/* =============================================================
   2. STUBSERVERUAT_VSDETAILS
   Oracle source: STUBSERVERUAT_VSDETAILS
   ============================================================= */

IF OBJECT_ID('${schema}.STUBSERVERUAT_VSDETAILS', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.STUBSERVERUAT_VSDETAILS (
        VSID                BIGINT NOT NULL,
        VSNAME              VARCHAR(255) NULL,
        PORT                INT NULL,
        LASTUPDATED         VARCHAR(30) NULL,
        USERNAME            VARCHAR(255) NULL,
        STATUS              VARCHAR(10) NULL,

        KEEPREQRESLOGS      VARCHAR(10) NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_KEEPREQRESLOGS DEFAULT 'No',

        KEEPREQRESLOGSDAYS  VARCHAR(20) NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_KEEPREQRESLOGSDAYS DEFAULT '15',

        SAVERESPTIME        VARCHAR(20) NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_SAVERESPTIME DEFAULT 'No',

        [GROUP]             VARCHAR(100) NULL,
        TAGS                VARCHAR(100) NULL,

        DELAYMODE           VARCHAR(50) NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_DELAYMODE DEFAULT 'FIXED',

        DELAY               BIGINT NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_DELAY DEFAULT 0,

        LOWERMS             BIGINT NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_LOWERMS DEFAULT 0,

        UPPERMS             BIGINT NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_UPPERMS DEFAULT 0,

        SIGMA               DECIMAL(10,4) NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_SIGMA DEFAULT 0.0,

        MEDIANMS            DECIMAL(10,4) NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_MEDIANMS DEFAULT 0.0,

        TOTALTXN            INT NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_TOTALTXN DEFAULT 0,

        DELAYPERCENT        INT NULL
            CONSTRAINT DF_STUBSERVERUAT_VSDETAILS_DELAYPERCENT DEFAULT 0,

        DATASOURCEENABLED   VARCHAR(10) NULL,

        CONSTRAINT PK_STUBSERVERUAT_VSDETAILS PRIMARY KEY (VSID)
    );
END;

/* =============================================================
   3. VS_LIVEURLS
   ============================================================= */

IF OBJECT_ID('${schema}.VS_LIVEURLS', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.VS_LIVEURLS (
        VSURLID     BIGINT NOT NULL,
        VSID        BIGINT NOT NULL,
        HOST        VARCHAR(200) NULL,
        ISACTIVE    VARCHAR(2) NULL,
        UPDATETIME  DATETIME NULL,
        UPDATEDBY   VARCHAR(50) NULL,
        CONSTRAINT PK_VS_LIVEURLS PRIMARY KEY (VSURLID)
    );
END;

/* =============================================================
   4. VS_EXECUTIONMODE
   Oracle MASTERID is VARCHAR2(100), so SQL Server uses VARCHAR(100)
   ============================================================= */

IF OBJECT_ID('${schema}.VS_EXECUTIONMODE', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.VS_EXECUTIONMODE (
        VSID            BIGINT NOT NULL,
        MASTERID        VARCHAR(100) NOT NULL,
        VIRTSERVER      VARCHAR(50) NULL,
        EXECUTIONMODE   VARCHAR(50) NULL,
        UPDATETIME      DATETIME NULL,
        UPDATEDBY       VARCHAR(50) NULL,
        CONSTRAINT PK_VS_EXECUTIONMODE PRIMARY KEY (VSID)
    );
END;

/* =============================================================
   5. READYAPI_VS_CATALOG
   ============================================================= */

IF OBJECT_ID('${schema}.READYAPI_VS_CATALOG', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.READYAPI_VS_CATALOG (
        MASTERID            BIGINT NOT NULL,
        VSNAME              VARCHAR(150) NOT NULL,
        TRANSPORTTYPE       VARCHAR(25) NOT NULL,
        VIRTSERVER          VARCHAR(50) NOT NULL,
        PORT                BIGINT NULL,
        UPDATETIME          DATETIME NULL,
        UPDATEDBY           VARCHAR(50) NULL,
        STATUS              VARCHAR(50) NULL,
        PARTNER             VARCHAR(300) NULL,
        TABLENAME           VARCHAR(100) NULL,
        INTERNAL_EXTERNAL   VARCHAR(10) NULL,
        ENV_TYPE            VARCHAR(20) NULL,
        HEALTHCHECK         VARCHAR(10) NULL,
        [GROUP]             VARCHAR(100) NULL,
        TAGS                VARCHAR(100) NULL,
        CONSTRAINT PK_READYAPI_VS_CATALOG PRIMARY KEY (MASTERID)
    );
END;

/* =============================================================
   6. STUBSERVER_MASTER_CATALOG_QA
   ============================================================= */

IF OBJECT_ID('${schema}.STUBSERVER_MASTER_CATALOG_QA', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.STUBSERVER_MASTER_CATALOG_QA (
        MASTERID            BIGINT NOT NULL,
        VSNAME              VARCHAR(150) NOT NULL,
        UPDATETIME          DATETIME NULL,
        STATUS              VARCHAR(50) NULL,
        BACKENDAPPLICATION  VARCHAR(300) NULL,
        BACKENDTYPE         VARCHAR(10) NULL,
        ENV_TYPE            VARCHAR(20) NULL,
        [GROUP]             VARCHAR(100) NULL,
        PORT                INT NULL,
        CONSTRAINT PK_STUBSERVER_MASTER_CATALOG_QA PRIMARY KEY (MASTERID)
    );
END;

/* =============================================================
   7. STUBSERVER_MASTER_CATALOG_UAT
   ============================================================= */

IF OBJECT_ID('${schema}.STUBSERVER_MASTER_CATALOG_UAT', 'U') IS NULL
BEGIN
    CREATE TABLE ${schema}.STUBSERVER_MASTER_CATALOG_UAT (
        MASTERID            BIGINT NOT NULL,
        VSNAME              VARCHAR(150) NOT NULL,
        UPDATETIME          DATETIME NULL,
        STATUS              VARCHAR(50) NULL,
        BACKENDAPPLICATION  VARCHAR(300) NULL,
        BACKENDTYPE         VARCHAR(10) NULL,
        ENV_TYPE            VARCHAR(20) NULL,
        [GROUP]             VARCHAR(100) NULL,
        PORT                INT NULL,
        CONSTRAINT PK_STUBSERVER_MASTER_CATALOG_UAT PRIMARY KEY (MASTERID)
    );
END;
