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
    private final String QUEUE_NAME;
    private final String FILE_PATH;

    public RedisFiller(Jedis jedis, String QUEUE_NAME, String FILE_PATH) {
    	this.jedis = jedis;
	this.QUEUE_NAME = QUEUE_NAME;
	this.FILE_PATH = FILE_PATH;
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
	for (String current : getFromFile(FILE_PATH))
	{
	    jedis.sadd(QUEUE_NAME, current);
	}
	System.out.println(jedis.scard(QUEUE_NAME));
	logger.info("Redis queue filled up");
	jedis.close();
    }
}
