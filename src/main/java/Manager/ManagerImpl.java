package Manager;

import dao.DaoMySql;
import initialization.PropertyLoader;
import initialization.RedisFiller;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import translator.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ManagerImpl implements Manager
{
    final static Logger logger = Logger.getLogger(ManagerImpl.class);

    private ExecutorService service;
    private Lock lock;
    private DaoMySql dao;
    private Jedis jedis;
    private int workersCount;

    private PropertyLoader propertyLoader;

    public ManagerImpl() {
	init();
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
        logger.info("Manager initiated");
    }

    public void startWorking()
    {
	new RedisFiller(jedis,
                        this.propertyLoader.getQueueName(),
                        this.propertyLoader.getFilePath())
                        .fillUpQueue();

	for (int i = 0; i < workersCount; i++)
	{
	    logger.info("Manager starts "+ i + " worker");
	    this.service.submit(new Worker(this.propertyLoader.getYandexUrl(),
                            this.propertyLoader.getYandexKey(),
                            this));
	}
    }

    @Override
    public void submitTranslation(List<String> origins, List<String> translations)
    {
	lock.lock();
	if (origins.size() != translations.size()) {
	    logger.error("Translations size is not equals to origins size!!!");
	    return;
	}
	this.dao.add(origins, translations);
	lock.unlock();
    }

    @Override
    public List<String> getNextWord()
    {
	lock.lock();
	List<String> nextWord = new ArrayList<>();

	for(int i = 0; i < this.propertyLoader.getBatchSize(); i++) {
		nextWord.add(this.jedis.spop(this.propertyLoader.getQueueName()));
	}

	if (jedis.dbSize() == 0) {
	    jedis.close();
	    service.shutdown();
	}
	lock.unlock();
	return nextWord;
    }
}