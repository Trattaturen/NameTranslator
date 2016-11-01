package initialization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisFiller
{
    private Jedis jedis;

    public RedisFiller(Jedis jedis) {
    	this.jedis = jedis;
    }

    final static Logger logger = Logger.getLogger(RedisFiller.class);

    public static List<String> getFromFile(String path)
    {
	List<String> wordsFromFile = new ArrayList<>();
	try (BufferedReader in = new BufferedReader(new FileReader(path)))
	{
	    String init;
	    while ((init = in.readLine()) != null)
	    {
		wordsFromFile.addAll(Arrays.asList(init.split(" ")));
	    }
	} catch (IOException e1)
	{
	    e1.printStackTrace();
	}
	return wordsFromFile;
    }

    public void fillUpQueue()
    {
	for (String current : getFromFile("/home/user/names_test.txt"))
	{
	    jedis.sadd("names", current);
	}
	System.out.println(jedis.scard("names"));
	logger.info("Redis queue filled up");
	jedis.close();
    }
}
