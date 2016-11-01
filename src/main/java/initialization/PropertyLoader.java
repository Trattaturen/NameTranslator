package initialization;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PropertyLoader
{
    final static Logger logger = Logger.getLogger(PropertyLoader.class);
    private static PropertyLoader propertyLoader;
    private Properties property;

    public static PropertyLoader getInstance()
    {
	if (propertyLoader == null)
	{
	    propertyLoader = new PropertyLoader();
	}
	return propertyLoader;

    }

    private PropertyLoader() {
	init();
    }

    private void init()
    {
	logger.info("Initializing properties");
	FileInputStream fis;
	property = new Properties();

	try
	{
	    fis = new FileInputStream("src/main/resources/config.properties");
	    property.load(fis);
	} catch (IOException e)
	{
	    logger.error("Problems with properties file");
	}
    }

    public String getYandexUrl()
    {
	return property.getProperty("yandex.url");
    }

    public String getYandexKey()
    {
	return property.getProperty("yandex.key");
    }

    public String getYandexLimitDaily()
    {
	return property.getProperty("yandex.limit.daily");
    }

    public String getYandexLimitMonthly()
    {
	return property.getProperty("yandex.limit.daily");
    }

    public String getJdbcDriver()
    {
	return property.getProperty("jdbc.driver");
    }

    public String getMySqlUser()
    {
	return property.getProperty("db.user");
    }

    public String getMySqlPass()
    {
	return property.getProperty("db.password");
    }

    public String getQueueHost()
    {
	return property.getProperty("queue.host");
    }

    public String getQueueName()
    {
	return property.getProperty("queue.name");
    }

    public String getWorkersCount()
    {
	return property.getProperty("workers.count");
    }

    public String getFilePath()
    {
	return property.getProperty("file.path");
    }
}
