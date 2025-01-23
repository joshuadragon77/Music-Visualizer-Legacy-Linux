public abstract class MultiThreader {
    
    int numberOfThreads;
    boolean active = true;

    ThreadObject[] threads;

    Object[] arguments;
    Object[] results;

    static class ThreadObject extends Thread{
        boolean activeThread = false;
        double percentageActive;
        int timeSpentInactive = 0;
        int timeSpentActive = 0;
        long timeSinceLastCheck = System.currentTimeMillis();
    }

    MultiThreader(int numberOfThreads) throws InterruptedException{
        this.numberOfThreads = numberOfThreads;
        threads = new ThreadObject[numberOfThreads];
        results = new Object[numberOfThreads];
        MultiThreader self = this;

        for (int i = 0;i<threads.length;i++){
            int threadId = i;
            ThreadObject thread = new ThreadObject(){
                public void run(){
                    try{
                        synchronized(this){
                            while (active){
                                long startTime = System.currentTimeMillis();
                                this.wait();
                                if (active == false)
                                    break;
                                timeSpentInactive += System.currentTimeMillis() - startTime;
                                if (System.currentTimeMillis()-timeSinceLastCheck > 1000){
                                    timeSinceLastCheck = System.currentTimeMillis();
                                    percentageActive = (double)timeSpentActive/(timeSpentActive + timeSpentInactive);
                                    timeSpentActive = 0;
                                    timeSpentInactive = 0;
                                }
                                startTime = System.currentTimeMillis();
                                results[threadId] = self.run(threadId, arguments);
                                timeSpentActive += System.currentTimeMillis() - startTime;
                                activeThread = false;
                                synchronized(self){
                                    self.notifyAll();
                                }
                                if (System.currentTimeMillis()-timeSinceLastCheck > 1000){
                                    timeSinceLastCheck = System.currentTimeMillis();
                                    percentageActive = (double)timeSpentActive/(timeSpentActive + timeSpentInactive);
                                    timeSpentActive = 0;
                                    timeSpentInactive = 0;
                                }
                            };
                        }
                    }
                    catch(Exception er){
                        System.out.println(String.format("An exception has occured with thread %d. This thread was forced to close.\n%s", threadId, er));
                        er.printStackTrace();
                        active = false;
                    };
                }
            };
            thread.start();
            threads[i] = thread;
        }
        Thread.sleep(10);
    }

    public double getThreadUsage(int threadId){
        return threads[threadId].percentageActive;
    }

    public Object[] start(Object[] args) throws InterruptedException{
        arguments = args;
        results = new Object[numberOfThreads];
        
        
        for (int i = 0;i<threads.length;i++){
            ThreadObject thread = threads[i];
            thread.activeThread = true;
            synchronized(thread){
                thread.notify();
            }
        }
        while (true){
            boolean finished = true;
            for (int i = 0;i<threads.length;i++){
                if (threads[i].activeThread){
                    synchronized(this){
                        this.wait();
                    }
                    finished = false;
                    break;
                }
            }
            if (finished)
                break;
        }
        return results;
    };

    public abstract Object run(int threadId, Object[] args);

    public static void main(String[] args) throws InterruptedException{
        MultiThreader mt = new MultiThreader(1000) {
            @Override
            public Object run(int threadId, Object[] args) {
                // TODO Auto-generated method stub
                try{
                    for (int i = 0;i<100;i++){
                        Thread.sleep(10);
                        System.out.print(i);
                    }
                    System.out.print("\nfinished");
                }
                catch(Exception er){};
                return threadId;
            }
        };
        Object[] a = mt.start(null);

        for (int i = 0;i<a.length;i++){
            System.out.println(a[i]);
        }
    }   
}
