package translator;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import Manager.Manager;

/**
 * Callable that handles translation via Yandex API
 *
 */
public class Worker implements Runnable
{
    final static Logger logger = Logger.getLogger(Worker.class);

    private static final MediaType REQUEST_MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded");
    // Constants, needed to parse request/response
    private static final String TEXT_KEY_REQUEST = "text=";
    private static final String TEXT_KEY_RESPONSE = "text";

    private String yandexUrl;
    private String yandexKey;

    private JsonParser jsonParser;
    private OkHttpClient client;
    private Manager manager;

    public Worker(String yandexUrl, String yandexKey, Manager manager) {
	logger.debug("Initializing Translator");
	this.jsonParser = new JsonParser();
	this.client = new OkHttpClient();
	this.yandexUrl = yandexUrl;
	this.yandexKey = yandexKey;
	this.manager = manager;
    }

    @Override
    public void run()
    {
	String[] toTranslate;
	while ((toTranslate = manager.getNextWord()) != null)
	{
	    String translated = null;
	    logger.debug("Creating request body");
	    String translateRequest = TEXT_KEY_REQUEST + String.join("|", toTranslate);
	    RequestBody body = RequestBody.create(REQUEST_MEDIA_TYPE_JSON, translateRequest);
	    logger.debug("Building request");
	    Request request = new Request.Builder().url(yandexUrl + yandexKey).post(body).build();

	    Response response;
	    try
	    {
		logger.debug("Posting request to yandex");
		response = client.newCall(request).execute();
		int responseCode = response.code();

		if (responseCode == 404)
		{
		    logger.error("Yandex API limit have been reached");
		    break;
		}

		logger.debug("Parsing response");
		JsonObject sourceObject = jsonParser.parse(response.body().string()).getAsJsonObject();
		translated = sourceObject.getAsJsonArray(TEXT_KEY_RESPONSE).get(0).getAsString();
		logger.debug("Got request from yandex");
	    } catch (IOException e)
	    {
		logger.warn("Exception while translating", e);
	    }
	    manager.submitTranslation(toTranslate, translated);
	}

    }
}