package base.adaptor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Adaptor extends Remote
{
    void returnJob(String origin, String translation) throws RemoteException;

    void setWorkersCount(int workersCount) throws RemoteException;

    void executeJob(List<String> job) throws RemoteException;
}
