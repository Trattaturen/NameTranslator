package base.adaptor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Adaptor extends Remote
{
    public void setWorkersCount(int workersCount) throws RemoteException;

    public void executeJob(List<String> job) throws RemoteException;

    public void init(String yandexUrl, String yandexKey, int workersCount) throws RemoteException;

    public void setYandexKey(String yandexKey) throws RemoteException;

}
