package p2p.utils;

import java.util.Random;

public class UploadUtils {

    public static Integer generateCode(){
        int DYNAMIC_STARTING_PORT = 49152;
        int DYNAMIC_ENDING_PORT = 65535;

        Random random = new Random();
        return random.nextInt(DYNAMIC_ENDING_PORT-DYNAMIC_STARTING_PORT) + DYNAMIC_STARTING_PORT;
    }

}