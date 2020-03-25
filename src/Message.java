import java.io.Serializable;

public class Message<T> implements Serializable{
    private static final long serialVersionUID = 1L;

    private T message;

    public Message(T message){
        this.message = message;
    }

    public T getMessage(){
        return message;
    }

    // public T getMessageObject(){
    //     try{
    //         if(!single){
    //             throw new IllegalStateException();
    //         }
    //         return (T) message;
    //     }catch(IllegalStateException e){
    //         System.err.println("Could not extract single object");
    //         return null;
    //     }
    // }

    // public List<T> getMessageList(){
    //     try{
    //         if(single){
    //             throw new IllegalStateException();
    //         }
    //         return (ArrayList<T>) message;
    //     }catch(IllegalStateException e){
    //         System.err.println("Could not extract list");
    //         return null;
    //     }
    // }

    // public void print(){
    //     if(single){
    //         System.out.println(message.toString());
    //     }else{
    //         for(T object : (ArrayList<T>)message){
    //             System.out.println(object.toString());
    //         }
    //     }
    // }
}