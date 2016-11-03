package base.util;

import java.io.*;
import java.util.*;

public class ApiKeysController
{
    private final String KEY_FILE_PATH = "src/main/resources/yandexApiKeys.txt";
    public ApiKeysController()
    {

    }

    public String getFreshKey() throws IOException
    {
        String freshKey = "";

	try (BufferedReader in = new BufferedReader(new FileReader(KEY_FILE_PATH)))
	{
	    freshKey = in.readLine();
	} catch (IOException e1)
	{
	    e1.printStackTrace();
	}
	moveKeyToTheEnd();

	return freshKey;
    }

    private void moveKeyToTheEnd() throws IOException
    {
	List<String> buffer = new ArrayList<>();
	try (BufferedReader in = new BufferedReader(new FileReader(KEY_FILE_PATH)))
	{
	    String init;
	    while ((init = in.readLine()) != null)
	    {
		buffer.add(init);
	    }
	} catch (IOException e1)
	{
	    e1.printStackTrace();
	}

	File rewritedFile = new File(KEY_FILE_PATH);
	FileOutputStream stream = new FileOutputStream(rewritedFile, false);

	buffer.add(buffer.remove(0));

	for (String key : buffer) {
	    stream.write((key + "\n").getBytes());
	}
    }
}