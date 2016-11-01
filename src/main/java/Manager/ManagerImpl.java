package Manager;

import dao.DaoMySql;
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

    private String queueHost;
    private String queueName;
    private String filePath;
    private int workersCount;

    public ManagerImpl() {
	loadProperties();
	init();
	startWorking();
    }

    private void init()
    {
	this.service = Executors.newFixedThreadPool(WORKERS_COUNT);
	this.lock = new ReentrantLock();
	this.dao = DaoMySql.getInstance();
	this.jedis = new Jedis(QUEUE_HOST);
    }

    private void loadProperties()
    {

    }

    private void startWorking()
    {
	new RedisFiller(jedis, QUEUE_NAME, FILE_PATH).fillUpQueue();

	for (int i = 0; i < WORKERS_COUNT; i++)
	{
	    this.service.submit(new Worker(this));
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
	String nextWord = this.jedis.spop(QUEUE_NAME);
	lock.unlock();
	return nextWord;
    }
}