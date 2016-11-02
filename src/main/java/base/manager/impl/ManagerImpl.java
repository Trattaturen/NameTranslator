package base.manager.impl;

import base.adaptor.Adaptor;
import base.util.DaoMySql;
import base.util.PropertyLoader;
import base.util.RedisFiller;
import redis.clients.jedis.Jedis;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManagerImpl extends UnicastRemoteObject implements base.manager.Manager
{
    private PropertyLoader propertyLoader;
    private Jedis jedis;
    private String queueName;
    private int jobCount;

    public ManagerImpl() throws RemoteException
    {
        super();
        init();
    }

    private void init() {
        this.propertyLoader = PropertyLoader.getInstance();

        this.queueName = this.propertyLoader.getQueueName();
        this.jobCount = this.propertyLoader.getJobCount();

        this.jedis = new Jedis(this.propertyLoader.getQueueHost());
        //fill queue with data
        new RedisFiller(jedis,
                        this.propertyLoader.getQueueName(),
                        this.propertyLoader.getFilePath())
                        .fillUpQueue();

        exportToRmi();
    }

    @Override public void addAdaptor(Adaptor adaptor) throws RemoteException
    {
        adaptor.init(this.propertyLoader.getYandexUrl(),
                        this.propertyLoader.getYandexKey(),
                        this.propertyLoader.getWorkersCount());
        giveJobToAdaptor(adaptor);
    }

    @Override public void onJobExecuted(Map<String, String> job, Adaptor adaptor) throws RemoteException
    {
        Thread pushToSql = new Thread(new DaoMySql(job));
        pushToSql.start();

        giveJobToAdaptor(adaptor);
    }

    @Override public String onKeyLimitReached(String apiKey) throws RemoteException
    {
        String freshKey = "";

        return freshKey;
    }

    private void giveJobToAdaptor(Adaptor adaptor) throws RemoteException
    {
        List<String> nextJob = new ArrayList<>();
        String word;
        int counter = 0;

        while ((word = this.jedis.spop(this.queueName)) != null && counter < jobCount) {
            nextJob.add(word);
        }

        if (nextJob.size() > 0) {
            adaptor.executeJob(nextJob);
        }
    }

    public void exportToRmi()
    {
        String address = "//" + this.propertyLoader.getManagerRmiHost() + ":"
                        + this.propertyLoader.getManagerRmiPort() + "/"
                        + this.propertyLoader.getManagerRmiName();
        try
        {
            Naming.rebind(address, this);
        } catch (RemoteException | MalformedURLException e)
        {
            e.printStackTrace();
        }
    }
}