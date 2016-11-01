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

    public String getProperty(String propertyName)
    {
	return property.getProperty(propertyName);
    }

    public static void main(String[] args)
    {
	System.out.println(PropertyLoader.getInstance().getProperty("yandex.key"));

    }
}
