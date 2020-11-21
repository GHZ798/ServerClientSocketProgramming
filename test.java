import java.io.*;

public class test {
    public static void main(String argv[]){
        File file = new File("/cgi_bin/basic.cgi");
        if(file.canExecute()){
            System.out.println("YES");
        } else {
            System.out.println("NO");
        }
    }
}

// 26, 27, 28, 33, 34