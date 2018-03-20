/**
 * (C) Copyright IBM Corp. 2017,2018,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.persistence.jdbc.test.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.derby.tools.ij;

import com.ibm.watsonhealth.fhir.persistence.jdbc.dao.api.FHIRDbDAO;
import com.ibm.watsonhealth.fhir.persistence.jdbc.dao.impl.FHIRDbDAOBasicImpl;
import com.ibm.watsonhealth.fhir.persistence.jdbc.exception.FHIRPersistenceDBConnectException;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;

/**
 * This utility class initializes and bootstraps a FHIR Derby database for unit testing. 
 * If an existing database is found in the target path, it is reused. If not, a new database is defined and the appropriate DDL for
 * table and index creation is run using Liquibase.
 * 
 * It's intended that this class be consumed by testNg tests in the fhir-persistence-jdbc project.
 * @author markd
 *
 */
public class DerbyInitializer {
	
	private static final String LIQUIBASE_CHANGE_LOG_PATH_TEMPLATE = "../fhir-schemaddl/src/main/resources/liquibase/ddl/derby/<schemaType>-schema/fhirserver.derby.<schemaType>.xml";
	
	private boolean newDbCreated = false;
	private Properties dbProps;
	
	/**
	 * Main method to facilitate standalone testing of this class.
	 * @param args
	 */
	public static void main(String[] args) {
		DerbyInitializer initializer = new DerbyInitializer();
		try {
			initializer.bootstrapDb(false);
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructs a new DerbyInitializer using default database properties.
	 */
	public DerbyInitializer() {
		super();
		this.dbProps = new Properties();
		this.dbProps.setProperty(FHIRDbDAO.PROPERTY_DB_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
		this.dbProps.setProperty(FHIRDbDAO.PROPERTY_DB_URL, "jdbc:derby:target/fhirDB");
	}
	
	/**
	 * Constructs a new DerbyInitializer using the passed database properties.
	 */
	public DerbyInitializer(Properties props) {
		super();
		this.dbProps = props;
	}
	
	/**
	 * 
	 * Install Java Stored Procedure jar file into derby DB
	 */
	protected static boolean runScript(String commands, Connection connection) {
		InputStream inStream = new BufferedInputStream( new ReaderInputStream( new StringReader(commands)));
	    try { 
	        int result  = ij.runScript(connection,inStream,"UTF-8",System.out,"UTF-8"); 
	        return (result==0); 
	    } catch (IOException e) { 
	        return false; 
	    } finally { 
	        if(inStream!=null) { 
	            try { 
	            	inStream.close(); 
	            } 
	            catch (IOException e) { 
	            } 
	        }
	    } 
	} 
	
	/**
	 * Establishes a connection to fhirDB. Creates the database if necessary complete with tables indexes.
	 * @throws FHIRPersistenceDBConnectException
	 * @throws LiquibaseException
	 * @throws SQLException 
	 */
	public void bootstrapDb(boolean forceRunLiquibaseUpdates) throws FHIRPersistenceDBConnectException, LiquibaseException, SQLException {
		
		Connection connection = this.establishDb(forceRunLiquibaseUpdates);
		String schemaType = this.dbProps.getProperty(FHIRDbDAO.PROPERTY_SCHEMA_TYPE);
		if (this.isNewDbCreated()) {
			if(schemaType.equalsIgnoreCase("basic")) {
				this.runDDL(connection);
			} else if(schemaType.equalsIgnoreCase("normalized")) {
				String derbySprocJarDir = "../fhir-derby-sproc/target";
			    Collection<File> derbyJarFiles = FileUtils.listFiles(new File(derbySprocJarDir), new WildcardFileFilter("fhir-derby-sproc-*.jar"), null);
			    
			    if(!derbyJarFiles.isEmpty()) {
			    	StringBuilder sb = new StringBuilder();
					sb.append("CALL sqlj.install_jar('" + derbySprocJarDir + "/" + derbyJarFiles.iterator().next().getName() + "', 'APP.FhirDerbySProcs',0);");
					sb.append("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.classpath','APP.FhirDerbySProcs');");
					if(runScript(sb.toString(), connection)) {
						this.runDDL(connection);
					}
			    }
			}
		} else {
		    if(schemaType.equalsIgnoreCase("basic")) {
		        resetBasicDB(connection);
		    }
        }
	}
	
	private void resetBasicDB(Connection connection) throws SQLException {
	    for (String table : new String[]{"PARAMETER", "RESOURCE"}) {
	        try(PreparedStatement stmt = createPreparedDeleteStatement(connection, table)) {
	            int rows = stmt.executeUpdate();
	            System.out.println("Deleted " + rows + " rows from table " + table);
	        }
        }
    }
	
	private PreparedStatement createPreparedDeleteStatement(Connection con, String table) throws SQLException {
	    return con.prepareStatement("DELETE FROM " + table);
	}

    /**
	 * Establishes a connection to a Derby fhirdb, located in the project's /target/fhirDB directoryfor basic schema or /derby/fhirDB directory for normalized schema.
	 * If the database already exists, a connection is returned to it. If not, a new Derby fhirdb is created
	 * and populated with the appropriate tables. 
	 * @return Connection - A connection to the project's /derby/fhirDB
	 * @throws FHIRPersistenceDBConnectException
	 */
	@SuppressWarnings("rawtypes")
	private Connection establishDb(boolean forceRunLiquibaseUpdates) throws FHIRPersistenceDBConnectException  {
		
		Connection connection = null;
		SQLException sqlEx;
		
		FHIRDbDAO dao = new FHIRDbDAOBasicImpl(this.dbProps);
		
		String schemaType = this.dbProps.getProperty(FHIRDbDAO.PROPERTY_SCHEMA_TYPE);
		
		try {
			if(forceRunLiquibaseUpdates) {
				if(schemaType.equalsIgnoreCase("basic")) {
					this.dbProps.setProperty(FHIRDbDAO.PROPERTY_DB_URL, "jdbc:derby:target/fhirDB;create=true");
				} else {
					this.dbProps.setProperty(FHIRDbDAO.PROPERTY_DB_URL, "jdbc:derby:derby/fhirDB;create=true");
				}
				dao = new FHIRDbDAOBasicImpl(this.dbProps);
				connection = dao.getConnection();
				this.setNewDbCreated(true);
			} else {
				connection = dao.getConnection();
			}
			
		} 
		catch (FHIRPersistenceDBConnectException e) {
			if (e.getCause() != null && e.getCause() instanceof SQLException) {
				sqlEx = (SQLException) e.getCause();
				// XJ004 means database not found
				if("XJ004".equals(sqlEx.getSQLState())) {
					if(schemaType.equalsIgnoreCase("basic")) {
						this.dbProps.setProperty(FHIRDbDAO.PROPERTY_DB_URL, "jdbc:derby:target/fhirDB;create=true");
					} else {
						this.dbProps.setProperty(FHIRDbDAO.PROPERTY_DB_URL, "jdbc:derby:derby/fhirDB;create=true");
					}
					dao = new FHIRDbDAOBasicImpl(this.dbProps);
					connection = dao.getConnection();
					this.setNewDbCreated(true);
				}
			}
			else {
				throw e;
			}
		}
		return connection;
	}
	
	/**
	 * Uses Liquibase to run the required DDL for a newly created Derby database.
	 * The path to the Liquibase change log is defined by constant LIQUIBASE_CHANGE_LOG_PATH.
	 * @param dbConn
	 * @throws LiquibaseException
	 * @throws SQLException 
	 */
	private void runDDL(Connection dbConn) throws LiquibaseException, SQLException {
		
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(dbConn));
		
		String template = LIQUIBASE_CHANGE_LOG_PATH_TEMPLATE;
		
		Liquibase liquibase = new Liquibase(template.replaceAll("<schemaType>", this.dbProps.getProperty(FHIRDbDAO.PROPERTY_SCHEMA_TYPE)), new FileSystemResourceAccessor(), database);
		
		liquibase.update((Contexts)null);
		
		dbConn.setAutoCommit(true);
	}

	private boolean isNewDbCreated() {
		return newDbCreated;
	}

	private void setNewDbCreated(boolean newDbCreated) {
		this.newDbCreated = newDbCreated;
	}

}
