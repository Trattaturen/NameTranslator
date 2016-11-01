package Manager;

import dao.DaoMySql;
import initialization.PropertyLoader;
import initialization.RedisFiller;
import redis.clients.jedis.Jedis;
import translator.Worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ManagerImpl implements Manager
{
    private ExecutorService service;
    private Lock lock;
    private DaoMySql dao;
    private Jedis jedis;
    private int workersCount;

    private PropertyLoader propertyLoader;

    public ManagerImpl() {
	init();
	startWorking();
    }

    private void init()
    {
        this.propertyLoader = PropertyLoader.getInstance();

        this.workersCount = this.propertyLoader.getWorkersCount();
	this.service = Executors.newFixedThreadPool(this.workersCount);
	this.lock = new ReentrantLock();
	this.dao = new DaoMySql(this.propertyLoader.getJdbcDriver(),
                        this.propertyLoader.getMySqlUrl(),
                        this.propertyLoader.getMySqlUser(),
                        this.propertyLoader.getMySqlPass());
	this.jedis = new Jedis(this.propertyLoader.getQueueHost());
    }

    private void startWorking()
    {
	new RedisFiller(jedis,
                        this.propertyLoader.getQueueName(),
                        this.propertyLoader.getFilePath())
                        .fillUpQueue();

	for (int i = 0; i < workersCount; i++)
	{
	    this.service.submit(new Worker(this.propertyLoader.getYandexUrl(),
                            this.propertyLoader.getYandexKey(),
                            this.propertyLoader.getYandexLimitDaily(),
                            this.propertyLoader.getYandexLimitMonthly(), this));
	}
    }

    @Override
    public void submitTranslation(String origin, String translation)
    {
	lock.lock();
	this.dao.add(origin, translation);
	lock.unlock();
    }

    @Override
    public String getNextWord()
    {
	lock.lock();
	String nextWord = this.jedis.spop(this.propertyLoader.getQueueName());
	lock.unlock();
	return nextWord;
    }
}