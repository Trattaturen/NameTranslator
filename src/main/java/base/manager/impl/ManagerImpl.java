package base.manager.impl;

import base.adaptor.Adaptor;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManagerImpl implements base.manager.Manager
{
    private List<Adaptor> adaptors;

    public ManagerImpl() {
        this.adaptors = new ArrayList<>();
    }

    @Override public void addAdaptor(Adaptor adaptor) throws RemoteException
    {

    }

    @Override public void onJobExecuted(Map<String, String> job) throws RemoteException
    {

    }

    @Override public String onKeyLimitReached(String apiKey) throws RemoteException
    {
	return null;
    }
}