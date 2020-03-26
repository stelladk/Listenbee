import java.io.Serializable;
import java.math.BigInteger;

public class DemoBroker implements Serializable{
    private static final long serialVersionUID = 1L;
    public String IP;
    public BigInteger HASH_VALUE;
    
    private static final int InnerPORT = 1999; //port for publishers and inner broker communication
    private static final int ConsumerPORT = 2000;

    public DemoBroker(String IP){
        this.IP = IP;
        this.HASH_VALUE = Utilities.SHA1(IP+""+InnerPORT);
    }

    public String getIP(){
        return IP;
    }

    public BigInteger getHash(){
        return HASH_VALUE;
    }
    
    public static int getInnerPORT(){
        return InnerPORT;
    }

    public static int getConsumerPORT(){
        return ConsumerPORT;
    }

    @Override
    public String toString(){
        return "Broker@"+IP+"@"+InnerPORT+"@"+ConsumerPORT+"@"+HASH_VALUE;
    }

    public boolean equals(Broker broker){
        if(IP==broker.getIP()){
            return true;
        }
        return false;
    }

    public boolean equals(DemoBroker broker){
        if(IP==broker.getIP()){
            return true;
        }
        return false;
    }
}