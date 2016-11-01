package translator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.*;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Callable that handles translation via Yandex API
 *
 */
public class Worker implements Runnable
{
    final static Logger logger = Logger.getLogger(Worker.class);

    private static final int YANDEX_LIMIT_DAILY = 1000000;
    /// private static final int YANDEX_LIMIT_MONTHLY = 10000000;
    private int translatedSymbols;
    private static final String YANDEX_URL = "https://translate.yandex.net/api/v1.5/tr.json/translate?lang=en&key=";
    private static final String YANDEX_KEY = "trnsl.1.1.20161020T120319Z.1eef5b89221649d3.79a09b2042b8bd60500bbce401514cbe065e930b";
    private static final MediaType REQUEST_MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded");
    // Constants, needed to parse request/response
    private static final String TEXT_KEY_REQUEST = "text=";
    private static final String TEXT_KEY_RESPONSE = "text";

    private JsonParser jsonParser;
    private OkHttpClient client;

    public Worker(Manager manager) {
	logger.debug("Initializing Translator");
	jsonParser = new JsonParser();
	// Simple http client library
	client = new OkHttpClient();
    }

    @Override
    public void run()
    {
	String toTranslate = manager.getNextWord();
	while (toTranslate != null)
	{
	    translatedSymbols += toTranslate.length();
	    if (translatedSymbols >= YANDEX_LIMIT_DAILY)
	    {
		logger.error("Reached daily translation limit");
		break;
	    }
	    String translated = null;
	    logger.debug("Creating request body");
	    String translateRequest = TEXT_KEY_REQUEST + toTranslate;
	    RequestBody body = RequestBody.create(REQUEST_MEDIA_TYPE_JSON, translateRequest);
	    logger.debug("Building request");
	    Request request = new Request.Builder().url(YANDEX_URL + YANDEX_KEY).post(body).build();

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