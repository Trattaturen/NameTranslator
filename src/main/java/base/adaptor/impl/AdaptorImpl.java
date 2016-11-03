package base.adaptor.impl;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import base.adaptor.Adaptor;
import base.adaptor.worker.Worker;
import base.manager.Manager;

public class AdaptorImpl extends UnicastRemoteObject implements Adaptor
{
    final static Logger logger = Logger.getLogger(AdaptorImpl.class);
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MANAGER_RMI_HOST = "rmi://192.168.1.242:1098/NameTranslatorManager";

    private String yandexUrl;
    private String yandexKey;

    private int keyExpirationCallsCount;
    private List<String> job;
    private Map<String, String> translatedJob;
    private String managerHost;
    private ExecutorService service;
    private Manager manager;
    private int workersCount;

    public AdaptorImpl() throws RemoteException {
	logger.info("Initializing adaptor");
	managerHost = DEFAULT_MANAGER_RMI_HOST;
	try
	{
	    manager = (Manager) Naming.lookup(managerHost);
	    logger.info("Adding adaptor to manager");
	    manager.addAdaptor(this);
	} catch (MalformedURLException | RemoteException | NotBoundException e)
	{
	    logger.info("Problems with RMI", e);
	}
    }

    @Override
    public void init(String yandexUrl, String yandexKey, int workersCount) throws RemoteException
    {
	logger.info("Initializing executor service");
	this.translatedJob = new HashMap<>();
	this.yandexUrl = yandexUrl;
	this.yandexKey = yandexKey;
	this.workersCount = workersCount;
    }

    @Override
    public void executeJob(List<String> job) throws RemoteException
    {
	logger.info("Adaptor starting new Workers");
	this.service = Executors.newFixedThreadPool(this.workersCount);
	this.job = job;
	for (int i = 0; i < workersCount; i++)
	{
	    this.service.submit(new Worker(yandexUrl, yandexKey, this));
	}
    }

    public void continueJob()
    {
	logger.info("Continuing working with new key " + yandexKey);
	this.service = Executors.newFixedThreadPool(this.workersCount);
	for (int i = 0; i < workersCount; i++)
	{
	    this.service.submit(new Worker(yandexUrl, yandexKey, this));
	}
    }

    public synchronized String getNextJob()
    {
	logger.info("Searching for new job for worker. Currently in a list: " + job.size());
	if (!job.isEmpty())
	{
	    logger.info("Giving worker a new job");
	    return job.remove(0);

	} else
	{
	    try
	    {
		logger.info("Job list is empty. Shutting down service");
		service.awaitTermination(1, TimeUnit.SECONDS);
		logger.info("Service shutted down");
		logger.info("Giving all finished jobs to Manager. Size is " + translatedJob.size());
		manager.onJobExecuted(translatedJob, this);
		translatedJob.clear();
	    } catch (RemoteException e)
	    {
		logger.info("Problems with RMI", e);

	    } catch (InterruptedException e)
	    {
		logger.info("Problems with ExecutorService", e);
	    }
	}
	return null;
    }

    public synchronized void returnJob(String origin, String translation)
    {
	logger.info("Got translation from worker: " + origin + ":" + translation + ". Saving");
	translatedJob.put(origin, translation);
    }

    public void onKeyExpiration(String notDone)
    {
	logger.info("Got key expiration call number " + keyExpirationCallsCount);
	job.add(notDone);
	if (keyExpirationCallsCount == workersCount)
	{
	    logger.info("All workers called key expiration. Shutting down service");
	    this.service.shutdown();
	    try
	    {
		logger.info("Asking manager for a new key");
		manager.onKeyLimitReached(yandexKey);
	    } catch (RemoteException e)
	    {
		logger.info("Problems with RMI", e);
	    }
	    keyExpirationCallsCount = 0;
	    continueJob();
	}
    }

    @Override
    public void setWorkersCount(int workersCount) throws RemoteException
    {
	this.workersCount = workersCount;
    }

    @Override
    public void setYandexKey(String yandexKey) throws RemoteException
    {
	this.yandexKey = yandexKey;
    }

    public static void main(String[] args) throws RemoteException
    {
	@SuppressWarnings("unused")
	Adaptor adaptor = new AdaptorImpl();
    }
}