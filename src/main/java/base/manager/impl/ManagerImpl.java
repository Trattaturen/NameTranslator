package base.manager.impl;

import base.adaptor.Adaptor;
import base.util.ApiKeysController;
import base.util.DaoMySql;
import base.util.PropertyLoader;
import base.util.RedisFiller;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerImpl extends UnicastRemoteObject implements base.manager.Manager
{
    final static Logger logger = Logger.getLogger(ManagerImpl.class);

    private PropertyLoader propertyLoader;
    private Jedis jedis;
    private String queueName;
    private int jobCount;
    private int batchSize;

    public ManagerImpl() throws RemoteException
    {
	init();
    }

    public void start()
    {
	//fill queue with data
	new RedisFiller(jedis,
			this.propertyLoader.getQueueName(),
			this.propertyLoader.getFilePath()).fillUpQueue();
	exportToRmi();
    }

    private void init()
    {
	this.propertyLoader = PropertyLoader.getInstance();

	this.batchSize = this.propertyLoader.getBatchSize();
	this.queueName = this.propertyLoader.getQueueName();
	this.jobCount = this.propertyLoader.getJobCount();

	this.jedis = new Jedis(this.propertyLoader.getQueueHost());
    }

    @Override public void addAdaptor(Adaptor adaptor) throws RemoteException
    {
	logger.info("Adaptor connected");
	adaptor.init(this.propertyLoader.getYandexUrl(), this.propertyLoader.getYandexKey(), this.propertyLoader.getWorkersCount());
	giveJobToAdaptor(adaptor);
    }

    @Override public void onJobExecuted(Map<String, String> result, Adaptor adaptor) throws RemoteException
    {
	if (result.isEmpty())
	{
	    logger.info("Result is empty");
	    return;
	}

	pushResultToMysql(result);

	giveJobToAdaptor(adaptor);
    }

    @Override public String onKeyLimitReached(String apiKey) throws RemoteException
    {
	logger.info("Demanding new key!!!!!!!!!!!!!!!!!");
	String freshKey = null;
	try
	{
	    freshKey = new ApiKeysController().getFreshKey();
	} catch (IOException e)
	{
	    e.printStackTrace();
	}
	logger.info("Limited key : " + apiKey);
	logger.info("Fresh key : " + freshKey);
	return freshKey;
    }

    private void giveJobToAdaptor(Adaptor adaptor) throws RemoteException
    {
	List<String> nextJob = new ArrayList<>();
	String word;

	for (int i = 0; i < jobCount; i++)
	{
	    word = this.jedis.spop(this.queueName);
	    if (word != null)
	    {
		nextJob.add(word);
	    } else
	    {
		break;
	    }
	}

	if (!nextJob.isEmpty())
	{
	    adaptor.executeJob(nextJob);
	    logger.info("Pushing job to adaptor " + nextJob.size());
	}
    }

    private void exportToRmi()
    {
        String host = this.propertyLoader.getManagerRmiHost();
	String address ="rmi://" + this.propertyLoader.getManagerRmiHost() + ":"
			+ this.propertyLoader.getManagerRmiPort()
			+ "/" + this.propertyLoader.getManagerRmiName();
	try
	{
	    System.setProperty("java.rmi.server.hostname", host);
	    LocateRegistry.createRegistry(1098);
	    Naming.rebind(address, this);
	} catch (RemoteException | MalformedURLException e)
	{
	    e.printStackTrace();
	}
    }

    private void pushResultToMysql(Map<String, String> result)
    {
	logger.info("Pushing to SQL count : " + result.size());
	Map<String, String> subResult = new HashMap<>();

	for (Map.Entry<String, String> entry : result.entrySet())
	{
	    if (subResult.size() >= this.batchSize)
	    {
		Thread pusher = new Thread(new DaoMySql(subResult));
		pusher.start();
		subResult = new HashMap<>();
	    }
	    subResult.put(entry.getKey(), entry.getValue());
	}

	if (!subResult.isEmpty())
	{
	    Thread pusher = new Thread(new DaoMySql(subResult));
	    pusher.start();
	}
    }
}