package base.adaptor.worker;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import base.adaptor.impl.AdaptorImpl;

/**
 * Runnable that handles translation via Yandex API
 *
 */
public class Worker implements Runnable
{
    final static Logger logger = Logger.getLogger(Worker.class);

    private static final int YANDEX_LIMIT_CODE = 404;
    private static final MediaType REQUEST_MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded");
    // Constants, needed to parse request/response
    private static final String TEXT_KEY_REQUEST = "text=";
    private static final String TEXT_KEY_RESPONSE = "text";

    private String yandexUrl;
    private String yandexKey;

    private JsonParser jsonParser;
    private OkHttpClient client;
    private AdaptorImpl adaptorImpl;

    public Worker(String yandexUrl, String yandexKey, AdaptorImpl adaptorImpl) {
	logger.debug("Initializing Translator");
	this.yandexUrl = yandexUrl;
	this.yandexKey = yandexKey;
	this.jsonParser = new JsonParser();
	this.client = new OkHttpClient();
	this.adaptorImpl = adaptorImpl;
    }

    @Override
    public void run()
    {
	String toTranslate;
	logger.info("worker started " + Thread.currentThread().getName());
	while ((toTranslate = adaptorImpl.getNextJob()) != null)
	{
	    logger.debug("To translate " + toTranslate);
	    String translated = null;
	    logger.debug("Creating request body");
	    String translateRequest = TEXT_KEY_REQUEST + toTranslate;
	    logger.debug("translateRequest " + translateRequest);
	    RequestBody body = RequestBody.create(REQUEST_MEDIA_TYPE_JSON, translateRequest);
	    logger.debug("Building request " + "url: " + yandexUrl + " key: " + yandexKey);
	    Request request = new Request.Builder().url(yandexUrl + yandexKey).post(body).build();

	    logger.debug("request builded : " + request.toString());
	    Response response;
	    try
	    {
		logger.debug("Posting request to yandex");
		response = client.newCall(request).execute();
		int responseCode = response.code();

		if (responseCode == YANDEX_LIMIT_CODE)
		{
		    logger.error("Yandex API limit have been reached");
		    adaptorImpl.onKeyExpiration(toTranslate);
		    break;
		}
		logger.debug("Parsing response");
		JsonObject sourceObject = jsonParser.parse(response.body().string()).getAsJsonObject();
		logger.debug("source obj " + sourceObject);
		translated = sourceObject.getAsJsonArray(TEXT_KEY_RESPONSE).get(0).getAsString();
		logger.debug("Translated : " + translated);
	    } catch (IOException e)
	    {
		logger.warn("Exception while translating", e);
	    }
	    logger.info("worker giving finished job to adaptor " + toTranslate + ":" + translated + Thread.currentThread().getName());
	    this.adaptorImpl.returnJob(toTranslate, translated);
	}
	logger.info("Worker shutting down! " + Thread.currentThread().getName());

    }
}