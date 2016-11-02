package base.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;

public class DaoMySql implements Runnable
{
    final static Logger logger = Logger.getLogger(DaoMySql.class);

    private static final String ADD_NAMES_QUERY = "INSERT INTO names_translation(eng_word, ara_word) VALUES (?, ?);";

    private String driver;
    private String url;
    private String user;
    private String pass;

    private Map<String, String> words;

    public DaoMySql(Map<String, String> words) {
	logger.debug("Initializing MySql Dao");
	this.driver = PropertyLoader.getInstance().getJdbcDriver();
	this.url = PropertyLoader.getInstance().getMySqlUrl();
	this.user = PropertyLoader.getInstance().getMySqlUser();
	this.pass = PropertyLoader.getInstance().getMySqlPass();
	this.words = words;
    }

    private void add()
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
	    for (Map.Entry<String, String> entry : words.entrySet())
	    {
		preparedStatement.setString(1, entry.getKey());
		preparedStatement.setString(2, entry.getValue());
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

    @Override public void run()
    {
	add();
    }
}
