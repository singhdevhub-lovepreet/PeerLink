package p2p;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    /**
     * Rigorous Test :-)
     */
//    private void recieveFile(Integer inviteCode){
//        RecieverServer recieverServer = new RecieverServer();
//        try{
//            recieverServer.receiveFile("127.0.0.1", inviteCode, "test1.txt");
//        }catch (Exception ex){
//            System.err.println("File couldn't get");
//            // ignored
//        }
//    }

    @Test
    public void shouldAnswerWithTrue() {
//        FileSharer fileSharer = new FileSharer();
//        int inviteCode = fileSharer.offerFile("/Users/lovepreetsingh/Desktop/PeerLink/src/test/java/p2p/test.txt");
//        System.out.println("port generated " + inviteCode);
//        fileSharer.startFileServer(inviteCode);
//        Thread thread = new Thread(() -> recieveFile(inviteCode));
        assertTrue(true);
    }
}
