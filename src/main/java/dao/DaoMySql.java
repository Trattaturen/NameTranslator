package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * A singleton class, that provides access to MySQL DB
 */
public class DaoMySql
{
    final static Logger logger = Logger.getLogger(DaoMySql.class);

    private static DaoMySql daoMySQL;

    private static final String DEFAULT_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "pass";
    private static final String ADD_NAMES_QUERY = "INSERT INTO Names(eng_word) VALUES (?);";
    // private static final String GET_TOTAL_COUNT_QUERY = "SELECT COUNT(*) FROM
    // Articles;";

    private String driver;
    private String url;
    private String user;
    private String pass;

    /**
     * @return instance of a class (if it is not yet instantiated - creates it)
     */
    public static synchronized DaoMySql getInstance()
    {
	if (daoMySQL == null)
	{
	    daoMySQL = new DaoMySql();
	}
	return daoMySQL;
    }

    /**
     * Private constructor of class. Should only be called by getInstance()
     * method
     */
    private DaoMySql() {
	logger.debug("Initializing MySql Dao with default parameters");
	driver = DEFAULT_JDBC_DRIVER;
	url = DEFAULT_DB_URL;
	user = DEFAULT_USER;
	pass = DEFAULT_PASS;
	daoMySQL = this;
    }

    public void add(String name)
    {
	logger.debug("Adding article to MySql");
	Connection connection = null;

	try
	{
	    logger.debug("Getting JDBC driver");
	    Class.forName(driver);

	    logger.debug("Getting connection");
	    connection = DriverManager.getConnection(url, user, pass);

	    logger.debug("Preparing statement");
	    PreparedStatement preparedStatement = connection.prepareStatement(ADD_NAMES_QUERY);

	    logger.debug("Setting statement parameter");
	    preparedStatement.setString(1, name);

	    logger.debug("Executing statement");
	    preparedStatement.executeUpdate();

	} catch (SQLException se)
	{
	    logger.error("Exception while adding to MySql", se);

	} catch (Exception e)
	{
	    logger.error("Exception while adding to MySql", e);
	} finally
	{
	    try
	    {
		if (connection != null)
		    connection.close();
	    } catch (SQLException se)
	    {
		logger.error("Exception while closing connection", se);
	    }
	}
    }
}
