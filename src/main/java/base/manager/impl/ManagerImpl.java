package base.manager.impl;

import base.adaptor.Adaptor;
import base.util.DaoMySql;
import base.util.PropertyLoader;
import base.util.RedisFiller;
import com.sun.corba.se.spi.activation.Server;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
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

    public ManagerImpl() throws RemoteException
    {
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

//        exportToRmi();
    }

    @Override public void addAdaptor(Adaptor adaptor) throws RemoteException
    {
        logger.info("Adaptor connected");
        adaptor.init(this.propertyLoader.getYandexUrl(),
                        this.propertyLoader.getYandexKey(),
                        this.propertyLoader.getWorkersCount());
        giveJobToAdaptor(adaptor);
    }

    @Override public void onJobExecuted(Map<String, String> result, Adaptor adaptor) throws RemoteException
    {
        if (result.isEmpty()) {
            logger.info("Result is empty");
            return;
        }

        pushResultToMysql(result);

        giveJobToAdaptor(adaptor);
    }

    @Override public String onKeyLimitReached(String apiKey) throws RemoteException
    {
        String freshKey = "";
        logger.info("Demanding new key!!!!!!!!!!!!!!!!!");
        return freshKey;
    }

    private void giveJobToAdaptor(Adaptor adaptor) throws RemoteException
    {
        List<String> nextJob = new ArrayList<>();
        String word;

        for (int i = 0; i < jobCount; i++) {
            word = this.jedis.spop(this.queueName);
            if (word != null) {
                nextJob.add(word);
            } else {
                break;
            }
        }

        if (!nextJob.isEmpty()) {
            adaptor.executeJob(nextJob);
            logger.info("Pushing job to adaptor " + nextJob.size());
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
            logger.info("Manager has been exported to RMI: " + address);
        } catch (RemoteException | MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    private void pushResultToMysql(Map<String, String> result) {
        logger.info("Pushing to SQL count : " + result.size());
        Map<String, String> subResult = new HashMap<>();

        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (subResult.size() >= 500) {
                Thread pusher = new Thread(new DaoMySql(subResult));
                pusher.start();
                subResult = new HashMap<>();
            }
            subResult.put(entry.getKey(), entry.getValue());
        }

        if(!subResult.isEmpty()) {
            Thread pusher = new Thread(new DaoMySql(subResult));
            pusher.start();
        }
    }
}