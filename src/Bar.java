import javaSimulation.*;
import javaSimulation.Process;

/**
 *
 * @author Tomas
 */
public class Bar extends Process {
    static final double 
    SIM_PERIOD = 3600,
    DRINK_POISSON = 2.5,
    BEER_PREPARATION_TIME = 12,
    SHOT_PREPARATION_TIME = 8,
    CUSTOMER_EXPONENCIONAL = 1/30.0,
    SIMPLE_ORDER_P = 0.8,
    SIMPLE_ORDER_BEER_P = 0.75;
    
    Head couch = new Head();
    Head waitingLine = new Head();
    //One barman can serve beer for more customers while other barman make d
    Head beerLine = new Head();
    Random random = new Random(5);
    double throughTime;
    int noOfCustomers, maxLength, beerLineMaxLength, servedBeers, servedShots;
    int barmansCount, spigotsCount, freeSpigots;
    long startTime = System.currentTimeMillis();
    
    public Bar(int n, int beerSpigots){
        barmansCount = n;
        freeSpigots = spigotsCount = beerSpigots;
    }
    
    public void actions(){
        for(int i = 1; i <= barmansCount; i++)
            new Barman().into(couch);
        activate(new CustomerGenerator());
        hold(SIM_PERIOD + 1000000);
        report();
    }
    
    void report(){
        System.out.println(barmansCount + " barmans, "+spigotsCount+" spigots simulation");
        System.out.println("No. of customers through the system = " + noOfCustomers);
        System.out.println("Served beers/shots = " + servedBeers + "/" + servedShots);
        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance();
        fmt.setMaximumFractionDigits(2);
        System.out.println("Av.elapsed time = " + fmt.format(throughTime/noOfCustomers));
        System.out.println("Maximum queue length = " + maxLength);
        System.out.println("Maximum beer queue length = " + beerLineMaxLength);
        System.out.println("\nExecution time: "+
                fmt.format((System.currentTimeMillis() - startTime)/1000.0)+" secs.\n");
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        activate(new Bar(1, 1));
        activate(new Bar(1, 2));
        activate(new Bar(2, 1));
        activate(new Bar(2, 2));
        activate(new Bar(3, 1));
        activate(new Bar(3, 2));
        activate(new Bar(3, 3));
        activate(new Bar(4, 1));
        activate(new Bar(4, 2));
        activate(new Bar(4, 3));
        activate(new Bar(4, 4));
    }
    
    /****** Classes *******/
    class Customer extends Process {
        public int remain_beers, remain_shots;
        Customer(int beers, int shots){
            remain_beers = beers;
            remain_shots = shots;
        }
        
        public void actions(){
            double entryTime = time();
            into(waitingLine);
            int qLength = waitingLine.cardinal();
            if(maxLength < qLength)
                maxLength = qLength;
            if(!couch.empty())
                activate((Barman) couch.first());
            passivate();
            noOfCustomers++;
            throughTime += time() - entryTime;
        }
    }
    
    class Barman extends Process {
        public void actions(){
            while(true){
                out();
                do{
                    //first serve all beers
                    if(!beerLine.empty() && freeSpigots > 0){
                        freeSpigots--;
                        while(!beerLine.empty()){
                            Customer served = (Customer) beerLine.first();
                            served.out();
                            hold(served.remain_beers*BEER_PREPARATION_TIME);
                            servedBeers += served.remain_beers;
                            served.remain_beers = 0;
                            if(served.remain_shots == 0){
                                activate(served);
                            }
                        }
                        freeSpigots++;
                    }
                    //then accept new order
                    if(!waitingLine.empty()){
                        Customer served = (Customer) waitingLine.first();
                        served.out();
                        if(served.remain_beers > 0){
                            served.into(beerLine);
                            int qLength = beerLine.cardinal();
                            if(beerLineMaxLength < qLength)
                                beerLineMaxLength = qLength;
                            if(freeSpigots > 0 && !couch.empty()){
                                activate((Barman) couch.first());
                            }
                        }
                        if(served.remain_shots > 0){
                            hold(served.remain_shots*SHOT_PREPARATION_TIME);
                            servedShots += served.remain_shots;
                            served.remain_shots = 0;
                            if(served.remain_beers == 0){
                                activate(served);
                            }
                        }
                    }
                }while(!waitingLine.empty() || (!beerLine.empty() && freeSpigots > 0));
                wait(couch);
            }
        }
    }
    
    class CustomerGenerator extends Process{
        public void actions(){
            while(time() <= SIM_PERIOD){
                int orderCount = random.poisson(DRINK_POISSON);
                //at least one drink
                if(orderCount < 1)
                    continue;
                int beerCount;
                //beers or shots
                if(!random.draw(SIMPLE_ORDER_P)){
                    if(!random.draw(SIMPLE_ORDER_BEER_P)){
                        //only beers
                        beerCount = orderCount;
                    }else{
                        //only shots
                        beerCount = 0;
                    }
                }else{
                    //both?
                    beerCount = random.nextInt(orderCount+1);
                }
                activate(new Customer(beerCount, orderCount - beerCount));
                hold(random.negexp(CUSTOMER_EXPONENCIONAL));
            }
        }
    }
}
