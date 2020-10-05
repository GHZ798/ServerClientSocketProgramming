import java.io.*;

public class test {
    public static void main(String argv[]){
        File file = new File("./resources/ls");
        file.setReadable(false);
        System.out.println("Readable: " + file.canRead()); 
        if(file.canRead()){
            System.out.println(file.getName());
        }
    }
}
