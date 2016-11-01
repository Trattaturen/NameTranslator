package translator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.*;
import org.apache.log4j.Logger;

import Manager.Manager;

import java.io.IOException;

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

    private static int translatedSymbols;

    private String yandexUrl;
    private String yandexKey;
    private int yandexDailyLimit;
    private int yandexMonthlyLimit;

    private JsonParser jsonParser;
    private OkHttpClient client;
    private Manager manager;

    public Worker(String yandexUrl, String yandexKey, int yandexDaiyLimit, int yandexMonthlyLimit, Manager manager) {
	logger.debug("Initializing Translator");
	this.jsonParser = new JsonParser();
	// Simple http client library
	this.client = new OkHttpClient();
	this.manager = manager;
    }

    @Override
    public void run()
    {
	String toTranslate = manager.getNextWord();
	while (toTranslate != null)
	{
	    translatedSymbols += toTranslate.length();
	    if (translatedSymbols >= yandexDailyLimit)
	    {
		logger.error("Reached daily translation limit");
		break;
	    }
	    String translated = null;
	    logger.debug("Creating request body");
	    String translateRequest = TEXT_KEY_REQUEST + toTranslate;
	    RequestBody body = RequestBody.create(REQUEST_MEDIA_TYPE_JSON, translateRequest);
	    logger.debug("Building request");
	    Request request = new Request.Builder().url(yandexUrl + yandexKey).post(body).build();

	    Response response;
	    try
	    {
		logger.debug("Posting request to yandex");
		response = client.newCall(request).execute();
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