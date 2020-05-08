/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.tss.projectwork;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 *
 * @author alfonsodomenici
 */
@DataSourceDefinition(
        className = DbConfigurator.MARIADB_CLASS_NAME,
        name = DbConfigurator.DS_JNDI_NAME,
        serverName = DbConfigurator.MARIADB_HOST,
        portNumber = DbConfigurator.MARIADB_PORT,
        user = DbConfigurator.MARIADB_USR,
        password = DbConfigurator.MARIADB_USER_PWD,
        databaseName = DbConfigurator.MARIADB_DATABASE_NAME
)
@Singleton()
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DbConfigurator {

    public static final String MARIADB_HOST = "localhost";
    public static final int MARIADB_PORT = 3306;
    public static final String MARIADB_PROTOCOL = "tcp";
    public static final String MARIADB_ROOT_PWD = "root";
    public static final String MARIADB_USR = "pwapp";
    public static final String MARIADB_USER_PWD = "pwapp";
    public static final String MARIADB_DATABASE_NAME = "projectwork";
    public static final String MARIADB_CLASS_NAME = "org.mariadb.jdbc.MariaDbDataSource";
    public static final String DS_JNDI_NAME = "java:global/jdbc/pw";

    @Resource(lookup = DS_JNDI_NAME)
    private DataSource pw;

    private String jdbcBaseUrl;

    @PostConstruct
    public void init() {
        System.out.println("----------------------- Init DbConfiguration-----------------------");
        jdbcBaseUrl = "jdbc:mariadb://" + MARIADB_HOST + ":" + MARIADB_PORT + "/";
        checkCreateDb();
        checkDasource();
        //migrate();
        System.out.println("----------------------- End DbConfiguration-----------------------");
    }

    public void checkCreateDb() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DbConfigurator.class.getName()).log(Level.SEVERE, null, ex);
        }
        try ( Connection conn = DriverManager.getConnection(jdbcBaseUrl, "root", MARIADB_ROOT_PWD)) {
            // create a Statement
            try ( Statement stmt = conn.createStatement()) {
                //execute query
                try ( ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME"
                        + "  FROM INFORMATION_SCHEMA.SCHEMATA"
                        + " WHERE SCHEMA_NAME = '" + MARIADB_DATABASE_NAME + "'")) {
                    if (rs.next()) {
                        System.out.println("---------------- check database ok -------------------");
                        return;
                    }
                    stmt.executeUpdate("create database projectwork character set UTF8");
                    stmt.executeUpdate("grant all on projectwork.* to 'pwapp'@'%' identified by 'pwapp'");
                    System.out.println("---------------- database created -------------------");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DbConfigurator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void checkDasource() {
        try ( Connection connection = pw.getConnection()) {
            System.out.println(
                    connection.getMetaData().getDatabaseProductName() + "-"
                    + connection.getCatalog()
            );
            System.out.println("---------------- check datasource ok -------------------");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean migrate() {
        Flyway flyway = Flyway
                .configure()
                .dataSource(jdbcBaseUrl + MARIADB_DATABASE_NAME, MARIADB_USR, MARIADB_USER_PWD)
                .baselineOnMigrate(true)
                .load();
        int result = flyway.migrate();
        System.out.println("-------------------------result migrate------------- " + result);
        return result > 0;
    }

}
