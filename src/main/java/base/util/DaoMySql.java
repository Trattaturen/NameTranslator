package base.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

public class DaoMySql
{
    final static Logger logger = Logger.getLogger(DaoMySql.class);

    private static final String ADD_NAMES_QUERY = "INSERT INTO names_translation(eng_word, ara_word) VALUES (?, ?);";

    private String driver;
    private String url;
    private String user;
    private String pass;

    public DaoMySql(String driver, String url, String user, String pass) {
	logger.debug("Initializing MySql Dao");
	this.driver = driver;
	this.url = url;
	this.user = user;
	this.pass = pass;
    }

    public void add(List<String> originalName, List<String> translatedName)
    {
	logger.debug("Adding article to MySql");
	Connection connection = null;

	try
	{
	    logger.debug("Getting JDBC driver");
	    Class.forName(driver);

	    logger.info("Getting connection url " + url + " user " + user + " pass " + pass);
	    connection = DriverManager.getConnection(url, user, pass);

	    logger.debug("Preparing statement");
	    PreparedStatement preparedStatement = connection.prepareStatement(ADD_NAMES_QUERY);

	    logger.debug("Setting statement parameters");
	    for (int i = 0; i < originalName.size(); i++)
	    {
		preparedStatement.setString(1, originalName.get(i));
		preparedStatement.setString(2, translatedName.get(i));
		preparedStatement.addBatch();
	    }
	    preparedStatement.executeBatch();

	    logger.debug("Executing statement");
//	    preparedStatement.executeUpdate();

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
