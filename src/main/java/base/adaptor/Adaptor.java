package base.adaptor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Adaptor extends Remote
{
    void setWorkersCount(int workersCount) throws RemoteException;

    void executeJob(List<String> job) throws RemoteException;

    void init(String yandexUrl, String yandexKey, int workersCount) throws RemoteException;

    void setYandexKey(String yandexKey) throws RemoteException;

}
