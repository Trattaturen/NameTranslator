package base.manager;

import base.adaptor.Adaptor;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Manager extends Remote
{
    void addAdaptor(Adaptor adaptor) throws RemoteException;

    void onJobExecuted(Map<String, String> job, Adaptor adaptor) throws RemoteException;

    String onKeyLimitReached(String apiKey) throws RemoteException;
}