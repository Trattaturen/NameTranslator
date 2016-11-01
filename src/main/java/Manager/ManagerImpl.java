package Manager;
import initialization.RedisFiller;
import redis.clients.jedis.Jedis;
import translator.Worker;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagerImpl implements Manager
{
    private final String QUEUE_HOST = "localhost";
    private final int WORKERS_COUNT = 5;
    private ExecutorService service;

    public ManagerImpl() {
        init();
        startWorking();
    }

    private void init() {
        Jedis jedis = new Jedis(QUEUE_HOST);

        RedisFiller filler = new RedisFiller(jedis);
	filler.fillUpQueue();

        this.service = Executors.newFixedThreadPool(WORKERS_COUNT);
    }

    private void startWorking() {
        this.service.submit(new Worker(this));
    }

    @Override public void submitTranslation(String origin, String translatino)
    {

    }

    @Override public String getNextWord()
    {
	return null;
    }
}