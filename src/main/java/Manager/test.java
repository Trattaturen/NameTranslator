package Manager;

import Manager.Manager;

public class test
{
    public static void main(String...args) {
        long startTime = System.currentTimeMillis();
	ManagerImpl manager = new ManagerImpl();
	manager.startWorking();
	System.out.println("-------------Time" + (System.currentTimeMillis() - startTime));
    }
}
