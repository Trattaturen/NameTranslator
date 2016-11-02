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

import org.apache.log4j.Logger;

import base.adaptor.Adaptor;
import base.manager.Manager;
import base.adaptor.worker.Worker;

public class AdaptorImpl extends UnicastRemoteObject implements Adaptor
{
    final static Logger logger = Logger.getLogger(AdaptorImpl.class);
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MANAGER_RMI_HOST = "rmi://192.168.1.54:1099/Manager";

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
	    logger.info("Adding aaptor to manager");
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
	logger.info("Searching for new job for worker");
	if (job.isEmpty())
	{
	    try
	    {
		logger.info("No more jobs in adaptor. Giving all finished jobs to Manager");
		manager.onJobExecuted(translatedJob, this);
	    } catch (RemoteException e)
	    {
		logger.info("Problems with RMI", e);
	    }
	}
	logger.info("Giving worker a new job");
	return job.remove(0);

    }

    public void returnJob(String origin, String translation)
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
}