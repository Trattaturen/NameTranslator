package initialization;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import redis.clients.jedis.Jedis;

public class RedisFiller
{
    public static List<String> getFromFile(String path)
    {
	List<String> wordsFromFile = new ArrayList<>();
	try (
		BufferedReader in = new BufferedReader(new FileReader(path)))
	{
	    String init;
	    while ((init = in.readLine()) != null)
	    {
		wordsFromFile.addAll(Arrays.asList(init.split(" ")));
	    }
	} catch (FileNotFoundException e)
	{
	    e.printStackTrace();
	} catch (IOException e1)
	{
	    e1.printStackTrace();
	}
	return wordsFromFile;
    }

    public static void main(String[] args)
    {
	Jedis jedis = new Jedis("localhost");

	for (String current : getFromFile("/home/lebedev/names_test.txt"))
	{
	    jedis.sadd("names", current);
	}
	System.out.println(jedis.scard("names"));
	jedis.close();
    }
}
